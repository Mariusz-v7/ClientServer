package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;

public class ClientWatchdog implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientWatchdog.class);

    private static class Container {
        private final CommV2 comm;
        private final Socket socket;

        Container(CommV2 comm, Socket socket) {
            this.comm = comm;
            this.socket = socket;
        }
    }

    private final String name;
    private final long timeoutSeconds;
    final CopyOnWriteArraySet<Container> comms;
    final Semaphore semaphore;

    ClientWatchdog(String name, long timeoutSeconds) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
        comms = new CopyOnWriteArraySet<>();
        semaphore = new Semaphore(0);
        // TODO: remember to run it somewhere in factory
    }

    @Override
    public void run() {
        logger.info("[{}] Watchdog have started in thread: {}", name, Thread.currentThread().getName());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                semaphore.acquire();

                //semaphore.tryAcquire(); // TODO: use it as a sleep utility

                // TODO:
                /// if comm is ok -> leave in set and call semaphore release
                /// else -> remove from set and DONT call semaphore release
                // sleep
            } catch (InterruptedException e) {
                break;
            }
        }

        logger.info("[{}] Watchdog have finished in thread: {}", name, Thread.currentThread().getName());
    }

    boolean isTimeout(CommV2 comm) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);
        return comm.getLastDataReceived().isBefore(timeout) || comm.getLastDataSent().isBefore(timeout);
    }

    synchronized void register(CommV2 comm, Socket socket) {
        comms.add(new Container(comm, socket));
        semaphore.release();
    }

}
