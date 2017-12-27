package pl.mrugames.client_server.websocket_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactories;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.frames.WebSocketFrame;
import pl.mrugames.client_server.host.HostManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static HostManager hostManager;
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String... args) throws InterruptedException, IOException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        hostManager = new HostManager();

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientFactory<String, String, WebSocketFrame, WebSocketFrame> clientFactory = ClientFactories.createClientFactoryForWSServer(
                "WebSocket Host",
                60,
                new WebSocketWorkerFactory(Main::shutdown),
                executorService,
                1024
        );

        hostManager.newHost("Main Host", port, clientFactory, executorService);
        executorService.execute(hostManager);

        logger.info("Main finished...");
    }

    public static void shutdown() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
