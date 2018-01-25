package pl.mrugames.client_server.websocket_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactories;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.host.HostManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static HostManager hostManager;
    private static ExecutorService maintenanceExecutor = Executors.newCachedThreadPool();

    public static void main(String... args) throws InterruptedException, IOException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        hostManager = HostManager.create(4);

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientFactory<String, String> clientFactory = ClientFactories.createClientFactoryForWSServer(
                "WebSocket Host",
                60,
                new WebSocketWorkerFactory(Main::shutdown),
                maintenanceExecutor,
                1024
        );

        hostManager.newHost("Main Host", port, clientFactory);
        hostManager.run();

        logger.info("Main finished...");
    }

    public static void shutdown() {
        hostManager.shutdown();
        try {
            hostManager.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
