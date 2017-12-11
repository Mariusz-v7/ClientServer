package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ClientWatchdog implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientWatchdog.class);

    private static class Container {
        private final CommV2 comm;
        private final Socket socket;
        private final String clientName;

        Container(CommV2 comm, Socket socket, String clientName) {
            this.comm = comm;
            this.socket = socket;
            this.clientName = clientName;
        }
    }

    private final String name;
    private final long timeoutSeconds;
    private final CountDownLatch startSignal;
    final CopyOnWriteArraySet<Container> comms;
    final Semaphore semaphore;
    private volatile boolean running;

    public ClientWatchdog(String name, long timeoutSeconds) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
        this.startSignal = new CountDownLatch(1);
        comms = new CopyOnWriteArraySet<>();
        semaphore = new Semaphore(0);
    }

    @Override
    public void run() {
        try {
            running = true;
            startSignal.countDown();

            logger.info("[{}] Watchdog have started in thread: {}", name, Thread.currentThread().getName());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (semaphore.availablePermits() == 0) {
                        logger.info("[{}] There are no connections registered.", name);
                    }

                    semaphore.acquire();

                    logger.info("[{}] There are {} connections registered. Starting clean up.", name, comms.size());
                    check();
                    logger.info("[{}] Clean up finished. There are {} connections registered.", name, comms.size());

                } catch (InterruptedException e) {
                    break;
                }
            }
        } finally {
            running = false;
            logger.info("[{}] Watchdog have finished in thread: {}", name, Thread.currentThread().getName());
        }
    }

    long check() {
        long nextPossibleTimeout = -1;

        for (Container container : comms) {
            if (isTimeout(container.comm, container.clientName)) {
                logger.info("[{}] Connection is timed out, cleaning. Client: {}", name, container.clientName);

                try {
                    container.socket.close();
                    logger.info("[{}] Connection closed. Client: {}", name, container.clientName);
                } catch (Exception e) {
                    logger.error("[{}] Error during socket close. Client: {}", name, container.clientName, e);
                } finally {
                    comms.remove(container);
                }

                logger.info("[{}] Connection removed. Client: {}", name, container.clientName);
            } else {
                long nextTimeout = calculateSecondsToTimeout(container.comm);
                if (nextPossibleTimeout == -1 || nextPossibleTimeout > nextTimeout) {
                    nextPossibleTimeout = nextTimeout;
                }
            }
        }

        return nextPossibleTimeout;
    }

    boolean isTimeout(CommV2 comm, String clientName) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);
        if (comm.getLastDataReceived().isBefore(timeout)) {
            logger.info("[{}] Reception timeout. Client: {}", name, clientName);
            return true;
        }

        if (comm.getLastDataSent().isBefore(timeout)) {
            logger.info("[{}] Send timeout. Client: {}", name, clientName);
            return true;
        }

        return false;
    }

    long calculateSecondsToTimeout(CommV2 comm) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);

        long receiveTimeout = Duration.between(timeout, comm.getLastDataReceived()).toMillis();
        long sendTimeout = Duration.between(timeout, comm.getLastDataSent()).toMillis();

        double result = receiveTimeout > sendTimeout ? sendTimeout : receiveTimeout;
        result /= 1000.0;

        return (long) Math.ceil(result);
    }

    synchronized void register(CommV2 comm, Socket socket, String clientName) {
        comms.add(new Container(comm, socket, clientName));
        semaphore.release();
        logger.info("[{}] New connection has been registered. Client: {}", name, clientName);
    }

    public boolean isRunning() {
        return running;
    }

    boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return startSignal.await(timeout, timeUnit);
    }
}
