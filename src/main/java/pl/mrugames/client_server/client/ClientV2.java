package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.util.List;

class ClientV2 implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientV2.class);

    private final String name;
    private final List<Initializer> initializers;
    private final Runnable clientWorker;
    private final Runnable shutdownNotifier;

    ClientV2(String name, List<Initializer> initializers, Runnable clientWorker, Runnable shutdownNotifier) {
        this.name = name;
        this.initializers = initializers;
        this.clientWorker = clientWorker;
        this.shutdownNotifier = shutdownNotifier;

        logger.info("[{}] New client has been created", name);
    }

    @Override
    public void run() {
        logger.info("[{}] Client has been started in thread: {}", name, Thread.currentThread().getName());

        try {
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
            shutdownNotifier.run();
        }

        logger.info("[{}] Client has been terminated in thread: {}", name, Thread.currentThread().getName());
    }
}
