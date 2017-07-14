package pl.mrugames.client_server.object_server;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.helpers.ClientFactories;
import pl.mrugames.client_server.host.Host;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static Host host;

    public static void main(String... args) throws InterruptedException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientFactory clientFactory = ClientFactories.createClientFactoryForJavaServer("Main Host", 60, new WorkerFactory(Main::shutdown));

        host = new Host("Main Host", port, clientFactory);

        host.start();
        host.join();

        logger.info("Main finished...");
    }

    public static void shutdown() {
        host.interrupt();
    }
}
