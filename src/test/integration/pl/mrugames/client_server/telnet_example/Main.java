package pl.mrugames.client_server.telnet_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckReporter;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.helpers.ClientFactoryBuilder;
import pl.mrugames.client_server.client.io.TextReader;
import pl.mrugames.client_server.client.io.TextWriter;
import pl.mrugames.client_server.host.Host;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);
    private static Host host;

    public static void main(String... args) throws InterruptedException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        Thread reporter = HealthCheckReporter.createAndStart();


        ClientFactory<String, String, String, String> clientFactory = new ClientFactoryBuilder<>(
                TextWriter::new,
                TextReader::new,
                new ExampleClientWorkerFactory(Main::shutdown)
        )
                .setClientName("Main Client")
                .setTimeout(60)
                .build();

        host = new Host("Main Host", port, clientFactory);

        host.start();
        host.join();
        reporter.interrupt();

        logger.info("Main finished...");
    }

    private static void shutdown() {
        host.interrupt();
    }
}
