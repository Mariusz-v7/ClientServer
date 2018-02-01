package pl.mrugames.client_server.object_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactories;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.host.HostManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static HostManager hostManager;

    public static void main(String... args) throws InterruptedException, IOException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientFactory clientFactory = ClientFactories.createClientFactoryForJavaServer("Java server", 60, new WorkerFactory(Main::shutdown), 1024);

        hostManager = HostManager.create(4);
        hostManager.newHost("Main Host", port, clientFactory);

        hostManager.run();

        logger.info("Main finished...");
    }

    public static void shutdown() {
        try {
            hostManager.shutdown();
            hostManager.awaitTermination(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
