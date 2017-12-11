package pl.mrugames.client_server.object_client;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactories;
import pl.mrugames.client_server.client.ClientFactory;

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

        logger.info("Main started...");

        final String address = args[0];
        final int port = Integer.valueOf(args[1]);

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        ExecutorService executorService = Executors.newCachedThreadPool();

        ClientFactory clientFactory = ClientFactories.createClientFactoryForJavaServer("Local Client", 60, new WorkerFactory(), executorService);

        Client client = clientFactory.create(new Socket(address, port));

        client.awaitStop(1, TimeUnit.DAYS);

        executorService.shutdownNow();
    }
}
