package pl.mrugames.client_server.client;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.tasks.ClientShutdownTask;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ClientWatchdog implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientWatchdog.class);

    private final Timer cleanupMetric;
    private final CountDownLatch startSignal;
    final CopyOnWriteArraySet<Client> clients;
    final Semaphore semaphore;
    private volatile boolean running;

    public ClientWatchdog() {
        this.startSignal = new CountDownLatch(1);
        clients = new CopyOnWriteArraySet<>();
        semaphore = new Semaphore(0);

        cleanupMetric = Metrics.getRegistry().timer(MetricRegistry.name(ClientWatchdog.class, "cleanup"));
        Metrics.getRegistry().register(MetricRegistry.name(ClientWatchdog.class, "clients", "registered"), (Gauge<Integer>) clients::size);
        Metrics.getHealthCheckRegistry().register(MetricRegistry.name(ClientWatchdog.class, "running"), new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return running ? HealthCheck.Result.healthy("Watchdog is running") : HealthCheck.Result.unhealthy("Watchdog is not running");
            }
        });
    }

    @Override
    public void run() {
        // todo: replace logger with metrics - set logger  to debug
        try {
            running = true;
            startSignal.countDown();

            logger.info("Watchdog have started in thread: {}", Thread.currentThread().getName());

            long nextPossibleTimeout = -1;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (nextPossibleTimeout == -1) {
                        logger.info("There are no connections registered.");
                        semaphore.acquire();
                        semaphore.release();
                    } else {
                        int connections = clients.size();
                        logger.info("There are {} connections registered. Next possible timeout in: {} s.", connections, nextPossibleTimeout);

                        if (semaphore.tryAcquire(connections + 1, nextPossibleTimeout, TimeUnit.SECONDS)) {
                            logger.info("New connection has been registered. Running next check.");
                            semaphore.release(connections + 1);
                        } else {
                            logger.info("Possible timeout elapsed. Running next check.");
                        }
                    }

                    logger.info("There are {} connections registered. Starting clean up.", clients.size());
                    nextPossibleTimeout = check();
                    logger.info("Clean up finished. There are {} connections registered.", clients.size());

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
        try (Timer.Context ignored = cleanupMetric.time()) {

            long nextPossibleTimeout = -1;

            for (Client client : clients) {
                semaphore.acquire();

                if (isTimeout(client.getComm(), client.getName(), client.getTimeoutSeconds())) {
                    logger.info("Connection is timed out, cleaning. Client: {}", client.getName());

                    try {
                        client.getTaskExecutor().submit(new ClientShutdownTask(client));
                        logger.info("Connection closed. Client: {}", client.getName());
                    } catch (Exception e) {
                        logger.error("Error during channel close. Client: {}", client.getName(), e);
                    } finally {
                        clients.remove(client);
                    }

                    logger.info("Connection removed. Client: {}", client.getName());
                } else {
                    long nextTimeout = calculateSecondsToTimeout(client.getComm(), client.getTimeoutSeconds());
                    if (nextPossibleTimeout == -1 || nextPossibleTimeout > nextTimeout) {
                        nextPossibleTimeout = nextTimeout;
                    }

                    semaphore.release();
                }
            }

            return nextPossibleTimeout;
        }
    }

    boolean isTimeout(Comm comm, String clientName, long timeoutSeconds) {
        Instant timeout = Instant.now().minusSeconds(timeoutSeconds);
        if (comm.getLastDataReceived().isBefore(timeout)) {
            logger.info("Reception timeout. Client: {}", clientName);
            return true;
        }

        if (comm.getLastDataSent().isBefore(timeout)) {
            logger.info("Send timeout. Client: {}", clientName);
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
        logger.info("New connection has been registered. Client: {}", client.getName());
    }

    public boolean isRunning() {
        return running;
    }

    boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return startSignal.await(timeout, timeUnit);
    }
}
