package pl.mrugames.client_server.client_example;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactoryBuilder;
import pl.mrugames.client_server.client.ClientFactoryV2;
import pl.mrugames.client_server.client.ClientV2;
import pl.mrugames.client_server.client.io.TextReader;
import pl.mrugames.client_server.client.io.TextWriter;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws Exception {
        if (args.length != 2) {
            logger.error("Please provide address and port");
            return;
        }

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        logger.info("Main started...");

        final String address = args[0];
        final int port = Integer.valueOf(args[1]);

        ExecutorService executorService = Executors.newCachedThreadPool();

        ClientFactoryV2<String, String, String, String> clientFactory =
                new ClientFactoryBuilder<>(TextWriter::new, TextReader::new, new LocalClientWorkerFactory(), executorService)
                        .setName("Local Client")
                        .build();

        ClientV2 localClientWorker = clientFactory.create(new Socket(address, port));

        localClientWorker.awaitStop(1, TimeUnit.DAYS);

        executorService.shutdownNow();
    }
}
