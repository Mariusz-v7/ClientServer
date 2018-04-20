package pl.mrugames.nucleus.server.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.server.Metrics;
import pl.mrugames.nucleus.server.tasks.ClientShutdownTask;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class ConnectionWatchdog implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ConnectionWatchdog.class);

    private final CountDownLatch startSignal;
    final CopyOnWriteArraySet<Client> clients;
    final Semaphore semaphore;
    private volatile boolean running;
    private volatile Instant lastCycle;

    private final Counter acceptedConnections;
    private final Counter finishedConnections;
    private final Histogram connectionDuration;
    private final Meter cleanUpCycles;

    public ConnectionWatchdog() {
        this.startSignal = new CountDownLatch(1);
        clients = new CopyOnWriteArraySet<>();
        semaphore = new Semaphore(0);

        Metrics.getRegistry().register(name(ConnectionWatchdog.class, "clients", "registered"), (Gauge<Integer>) clients::size);
        Metrics.getHealthCheckRegistry().register(name(ConnectionWatchdog.class, "running"), new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return running ? HealthCheck.Result.healthy("Watchdog is running, last cycle: " + lastCycle) : HealthCheck.Result.unhealthy("Watchdog is not running, last cycle: " + lastCycle);
            }
        });

        acceptedConnections = Metrics.getRegistry().counter(name(ConnectionWatchdog.class, "accepted_connections"));
        finishedConnections = Metrics.getRegistry().counter(name(ConnectionWatchdog.class, "finished_connections"));
        connectionDuration = Metrics.getRegistry().histogram(name(ConnectionWatchdog.class, "connections_durations"));
        cleanUpCycles = Metrics.getRegistry().meter(name(ConnectionWatchdog.class, "cleanup_cycles"));
    }

    @Override
    public void run() {
        try {
            running = true;
            startSignal.countDown();

            logger.info("Watchdog have started in thread: {}", Thread.currentThread().getName());

            long nextPossibleTimeout = -1;
            while (!Thread.currentThread().isInterrupted()) {
                lastCycle = Instant.now();

                try {
                    cleanUpCycles.mark();
                    if (nextPossibleTimeout == -1) {
                        logger.debug("There are no connections registered.");
                        semaphore.acquire();
                        semaphore.release();
                    } else {
                        int connections = clients.size();
                        logger.debug("There are {} connections registered. Next possible timeout in: {} s.", connections, nextPossibleTimeout);

                        if (semaphore.tryAcquire(connections + 1, nextPossibleTimeout, TimeUnit.SECONDS)) {
                            logger.debug("New connection has been registered. Running next check.");
                            semaphore.release(connections + 1);
                        } else {
                            logger.debug("Possible timeout elapsed. Running next check.");
                        }
                    }

                    logger.debug("There are {} connections registered. Starting clean up.", clients.size());
                    nextPossibleTimeout = check();
                    logger.debug("Clean up finished. There are {} connections registered.", clients.size());

                } catch (InterruptedException e) {
                    break;
                }
            }
        } finally {
            running = false;
            logger.info("Watchdog have finished in thread: {}", Thread.currentThread().getName());
        }
    }

    long check() throws InterruptedException {

        long nextPossibleTimeout = -1;

        for (Client client : clients) {
            semaphore.acquire();

            if (isTimeout(client.getComm(), client.getName(), client.getConnectionTimeoutSeconds())) {
                logger.debug("Connection is timed out, cleaning. Client: {}", client.getName());

                try {
                    client.getTaskExecutor().submit(new ClientShutdownTask(client), client.getRequestTimeoutSeconds());
                    logger.debug("Connection closed. Client: {}", client.getName());
                } catch (Exception e) {
                    logger.error("Error during channel close. Client: {}", client.getName(), e);
                } finally {
                    clients.remove(client);
                    finishedConnections.inc();

                    long duration = Instant.now().toEpochMilli() - client.getCreated().toEpochMilli();
                    connectionDuration.update(duration);
                }

                logger.debug("Connection removed. Client: {}", client.getName());
            } else {
                long nextTimeout = calculateSecondsToTimeout(client.getComm(), client.getConnectionTimeoutSeconds());
                if (nextPossibleTimeout == -1 || nextPossibleTimeout > nextTimeout) {
                    nextPossibleTimeout = nextTimeout;
                }

                semaphore.release();
            }
        }

        return nextPossibleTimeout;
    }

    boolean isTimeout(Comm comm, String clientName, long timeoutSeconds) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);
        if (comm.getLastDataReceived().isBefore(timeout)) {
            logger.debug("Reception timeout. Client: {}", clientName);
            return true;
        }

        if (comm.getLastDataSent().isBefore(timeout)) {
            logger.debug("Send timeout. Client: {}", clientName);
            return true;
        }

        return false;
    }

    long calculateSecondsToTimeout(Comm comm, long timeoutSeconds) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);

        long receiveTimeout = Duration.between(timeout, comm.getLastDataReceived()).toMillis();
        long sendTimeout = Duration.between(timeout, comm.getLastDataSent()).toMillis();

        double result = receiveTimeout > sendTimeout ? sendTimeout : receiveTimeout;
        result /= 1000.0;

        return (long) Math.ceil(result);
    }

    synchronized void register(Client client) {
        clients.add(client);
        semaphore.release();
        logger.debug("New connection has been registered. Client: {}", client.getName());
        acceptedConnections.inc();
    }

    public boolean isRunning() {
        return running;
    }

    boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return startSignal.await(timeout, timeUnit);
    }
}
