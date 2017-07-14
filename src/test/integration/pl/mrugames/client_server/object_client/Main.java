package pl.mrugames.client_server.object_client;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.helpers.ClientFactories;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws InterruptedException, IOException, ExecutionException {
        if (args.length != 2) {
            logger.error("Please provide address and port");
            return;
        }

        logger.info("Main started...");

        final String address = args[0];
        final int port = Integer.valueOf(args[1]);

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        ClientFactory clientFactory = ClientFactories.createClientFactoryForJavaServer("Local Client", 60, new WorkerFactory());

        Worker localClientWorker = (Worker) clientFactory.create(new Socket(address, port)).get();

        localClientWorker.getShutdownSignal().await();

        clientFactory.shutdown();
    }
}
