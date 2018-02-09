package pl.mrugames.client_server.websocket_server;

import com.codahale.metrics.Slf4jReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.ClientFactories;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.host.HostManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static HostManager hostManager;
    private static Slf4jReporter reporter;

    public static void main(String... args) throws InterruptedException, IOException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        hostManager = HostManager.create(4);

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        reporter = Slf4jReporter.forRegistry(Metrics.getRegistry())
                .outputTo(logger)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        reporter.start(1, TimeUnit.MINUTES);

        ClientFactory<String, String> clientFactory = ClientFactories.createClientFactoryForWSServer(
                "WebSocket Host",
                60,
                30,
                new WebSocketWorkerFactory(Main::shutdown),
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
        } finally {
            reporter.report();
            reporter.stop();
        }
    }
}
