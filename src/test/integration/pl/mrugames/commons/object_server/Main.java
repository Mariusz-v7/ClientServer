package pl.mrugames.commons.object_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.helpers.ClientFactoryBuilder;
import pl.mrugames.commons.client.io.ObjectReader;
import pl.mrugames.commons.client.io.ObjectWriter;
import pl.mrugames.commons.host.Host;

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

        ClientFactory clientFactory = new ClientFactoryBuilder<>(ObjectWriter::new, ObjectReader::new, new WorkerFactory(Main::shutdown))
                .build();

        host = new Host("Main Host", port, clientFactory);

        host.start();
        host.join();

        logger.info("Main finished...");
    }

    public static void shutdown() {
        host.interrupt();
    }
}
