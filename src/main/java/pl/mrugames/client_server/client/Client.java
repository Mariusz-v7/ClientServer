package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final List<Initializer> initializers;
    private final Runnable clientWorker;
    private final SocketChannel channel;
    private final CountDownLatch startSignal;
    private final CountDownLatch shutdownSignal;
    private final Timer clientLifespanMetric;

    Client(String name, List<Initializer> initializers, Runnable clientWorker, SocketChannel channel, Timer clientLifespanMetric) {
        this.name = name;
        this.initializers = initializers;
        this.clientWorker = clientWorker;
        this.channel = channel;
        this.startSignal = new CountDownLatch(1);
        this.shutdownSignal = new CountDownLatch(1);
        this.clientLifespanMetric = clientLifespanMetric;

        logger.info("[{}] New client has been created", name);
    }

    @Override
    public void run() {
        logger.info("[{}] Client has been started in thread: {}", name, Thread.currentThread().getName());

        try (Timer.Context ignored = clientLifespanMetric.time()) {
            startSignal.countDown();

            for (Initializer initializer : initializers) {
                logger.info("[{}] {} initializer is starting", name, initializer.getClass().getSimpleName());
                initializer.run();
                logger.info("[{}] {} initializer has finished", name, initializer.getClass().getSimpleName());
            }

            logger.info("[{}] Starting client's main loop", name);

            clientWorker.run();

            logger.info("[{}] Client's main loop has finished", name);
        } catch (Exception e) {
            logger.info("[{}] Client finished with exception", name, e);
        } finally {
            closeChannel(channel);
            shutdownSignal.countDown();
        }

        logger.info("[{}] Client has been terminated in thread: {}", name, Thread.currentThread().getName());
    }

    public String getName() {
        return name;
    }

    List<Initializer> getInitializers() {
        return initializers;
    }

    Runnable getClientWorker() {
        return clientWorker;
    }

    public boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return startSignal.await(timeout, timeUnit);
    }

    public boolean awaitStop(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return shutdownSignal.await(timeout, timeUnit);
    }

    void closeChannel(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("Failed to close channel", e);
        }
    }
}
