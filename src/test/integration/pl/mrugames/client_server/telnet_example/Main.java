package pl.mrugames.client_server.telnet_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientFactoryBuilder;
import pl.mrugames.client_server.client.ProtocolFactory;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.LineReader;
import pl.mrugames.client_server.client.io.LineWriter;
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

        ClientFactory clientFactory = new ClientFactoryBuilder<>(new ExampleClientWorkerFactory(Main::shutdown), maintenanceExecutor,
                new ProtocolFactory<>(LineWriter::new, LineReader::new, FilterProcessor.EMPTY_FILTER_PROCESSOR, FilterProcessor.EMPTY_FILTER_PROCESSOR, "default")
        )
                .setName("Text Server")
                .build();

        hostManager.newHost("Main Host", port, clientFactory);

        hostManager.run();

        logger.info("Main finished...");
    }

    private static void shutdown() {
        try {
            hostManager.shutdown();
            hostManager.awaitTermination(1, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
