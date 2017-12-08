package pl.mrugames.client_server.object_server;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactoryV2;
import pl.mrugames.client_server.client.ClientWatchdog;
import pl.mrugames.client_server.client.filters.FilterProcessorV2;
import pl.mrugames.client_server.client.io.ObjectReader;
import pl.mrugames.client_server.client.io.ObjectWriter;
import pl.mrugames.client_server.host.HostManager;

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

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientWatchdog watchdog = new ClientWatchdog("test watchdog", 60);

        ClientFactoryV2 clientFactory = new ClientFactoryV2<>(
                "Test Factory",
                "Test Client",
                new WorkerFactory(Main::shutdown),
                Collections.emptyList(),
                ObjectWriter::new,
                ObjectReader::new,
                new FilterProcessorV2(Collections.emptyList()),
                new FilterProcessorV2(Collections.emptyList()),
                executorService,
                watchdog
        );

        executorService.execute(watchdog);

        hostManager = new HostManager();
        hostManager.newHost("Main Host", port, clientFactory);

        executorService.execute(hostManager);

        logger.info("Main finished...");
    }

    public static void shutdown() {
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
