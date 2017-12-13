package pl.mrugames.client_server.client;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class ClientWatchdog implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientWatchdog.class);

    private static class Container {
        private final Comm comm;
        private final SocketChannel channel;
        private final String clientName;

        Container(Comm comm, SocketChannel channel, String clientName) {
            this.comm = comm;
            this.channel = channel;
            this.clientName = clientName;
        }
    }

    private final Timer cleanupMetric;
    private final String name;
    private final long timeoutSeconds;
    private final CountDownLatch startSignal;
    final CopyOnWriteArraySet<Container> comms;
    final Semaphore semaphore;
    private volatile boolean running;

    ClientWatchdog(String name, long timeoutSeconds) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
        this.startSignal = new CountDownLatch(1);
        comms = new CopyOnWriteArraySet<>();
        semaphore = new Semaphore(0);

        cleanupMetric = Metrics.getRegistry().timer(MetricRegistry.name(ClientWatchdog.class, name, "cleanup"));
        Metrics.getRegistry().register(MetricRegistry.name(ClientWatchdog.class, "clients", "registered"), (Gauge<Integer>) comms::size);
        Metrics.getHealthCheckRegistry().register(MetricRegistry.name(ClientWatchdog.class, "running"), new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return running ? HealthCheck.Result.healthy("Watchdog is running") : HealthCheck.Result.unhealthy("Watchdog is not running");
            }
        });
    }

    @Override
    public void run() {
        try {
            running = true;
            startSignal.countDown();

            logger.info("[{}] Watchdog have started in thread: {}", name, Thread.currentThread().getName());

            long nextPossibleTimeout = -1;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (nextPossibleTimeout == -1) {
                        logger.info("[{}] There are no connections registered.", name);
                        semaphore.acquire();
                        semaphore.release();
                    } else {
                        int connections = comms.size();
                        logger.info("[{}] There are {} connections registered. Next possible timeout in: {} s.", name, connections, nextPossibleTimeout);

                        if (semaphore.tryAcquire(connections + 1, nextPossibleTimeout, TimeUnit.SECONDS)) {
                            logger.info("[{}] New connection has been registered. Running next check.", name);
                            semaphore.release(connections + 1);
                        } else {
                            logger.info("[{}] Possible timeout elapsed. Running next check.", name);
                        }
                    }

                    logger.info("[{}] There are {} connections registered. Starting clean up.", name, comms.size());
                    nextPossibleTimeout = check();
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

    long check() throws InterruptedException {
        try (Timer.Context ignored = cleanupMetric.time()) {

            long nextPossibleTimeout = -1;

            for (Container container : comms) {
                semaphore.acquire();

                if (isTimeout(container.comm, container.clientName)) {
                    logger.info("[{}] Connection is timed out, cleaning. Client: {}", name, container.clientName);

                    try {
                        closeChannel(container.channel);
                        logger.info("[{}] Connection closed. Client: {}", name, container.clientName);
                    } catch (Exception e) {
                        logger.error("[{}] Error during channel close. Client: {}", name, container.clientName, e);
                    } finally {
                        comms.remove(container);
                    }

                    logger.info("[{}] Connection removed. Client: {}", name, container.clientName);
                } else {
                    long nextTimeout = calculateSecondsToTimeout(container.comm);
                    if (nextPossibleTimeout == -1 || nextPossibleTimeout > nextTimeout) {
                        nextPossibleTimeout = nextTimeout;
                    }

                    semaphore.release();
                }
            }

            return nextPossibleTimeout;
        }
    }

    boolean isTimeout(Comm comm, String clientName) {
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

    long calculateSecondsToTimeout(Comm comm) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);

        long receiveTimeout = Duration.between(timeout, comm.getLastDataReceived()).toMillis();
        long sendTimeout = Duration.between(timeout, comm.getLastDataSent()).toMillis();

        double result = receiveTimeout > sendTimeout ? sendTimeout : receiveTimeout;
        result /= 1000.0;

        return (long) Math.ceil(result);
    }

    synchronized void register(Comm comm, SocketChannel channel, String clientName) {
        comms.add(new Container(comm, channel, clientName));
        semaphore.release();
        logger.info("[{}] New connection has been registered. Client: {}", name, clientName);
    }

    public boolean isRunning() {
        return running;
    }

    boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return startSignal.await(timeout, timeUnit);
    }

    void closeChannel(SocketChannel channel) throws IOException {
        channel.close();
    }
}
