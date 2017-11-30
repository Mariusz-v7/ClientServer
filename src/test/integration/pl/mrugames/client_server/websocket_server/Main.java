package pl.mrugames.client_server.websocket_server;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.helpers.ClientFactories;
import pl.mrugames.client_server.host.HostManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static HostManager hostManager;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String... args) throws InterruptedException, IOException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        hostManager = new HostManager();
        executorService.execute(hostManager);

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        ClientFactory clientFactory = ClientFactories.createClientFactoryForWSServer(
                "WS Server", 60, new WebSocketWorkerFactory(Main::shutdown));

        hostManager.newHost("Main Host", port, clientFactory);

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
