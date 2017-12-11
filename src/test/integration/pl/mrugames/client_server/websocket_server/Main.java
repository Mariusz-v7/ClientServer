package pl.mrugames.client_server.websocket_server;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactoryV2;
import pl.mrugames.client_server.client.ClientWatchdog;
import pl.mrugames.client_server.client.filters.FilterProcessorV2;
import pl.mrugames.client_server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.client_server.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.client_server.client.initializers.WebSocketInitializer;
import pl.mrugames.client_server.client.io.WebSocketReader;
import pl.mrugames.client_server.client.io.WebSocketWriter;
import pl.mrugames.client_server.host.HostManager;
import pl.mrugames.client_server.websocket.WebSocketHandshakeParser;

import java.io.IOException;
import java.util.Collections;
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

        ClientWatchdog clientWatchdog = new ClientWatchdog("Test watchdog", 60);
        executorService.execute(clientWatchdog);

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        ClientFactoryV2 clientFactory = new ClientFactoryV2<>(
                "Test factory",
                "Test client",
                new WebSocketWorkerFactory(Main::shutdown),
                Collections.singletonList(WebSocketInitializer.create(WebSocketHandshakeParser.getInstance())),
                WebSocketWriter::new,
                WebSocketReader::new,
                new FilterProcessorV2(Collections.singletonList(WebSocketFrameToStringFilter.getInstance())),
                new FilterProcessorV2(Collections.singletonList(StringToWebSocketFrameFilter.getInstance())),
                executorService,
                clientWatchdog
        );

        hostManager.newHost("Main Host", port, clientFactory);
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
