package pl.mrugames.client_server.websocket_server;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.helpers.ClientFactories;
import pl.mrugames.client_server.host.HostManager;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static HostManager hostManager = new HostManager();

    public static void main(String... args) throws InterruptedException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        ClientFactory clientFactory = ClientFactories.createClientFactoryForWSServer(
                "WS Server", 60, new WebSocketWorkerFactory(Main::shutdown));

        hostManager.newHost("Main Host", port, clientFactory);

        logger.info("Main finished...");
    }

    public static void shutdown() {
        try {
            hostManager.shutdown();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
