package pl.mrugames.commons.telnet_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.io.TextClientReader;
import pl.mrugames.commons.client.io.TextClientWriter;
import pl.mrugames.commons.host.Host;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);
    private static Host host;

    public static void main(String ...args) throws InterruptedException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientFactory clientFactory = new ClientFactory<>(
                "Main Client",
                60,
                TextClientWriter::new,
                TextClientReader::new,
                new ExampleClientWorkerFactory(Main::shutdown)
        );

        host = new Host("Main Host", port, clientFactory);

        host.start();
        host.join();

        logger.info("Main finished...");
    }

    private static void shutdown() {
        host.interrupt();
    }
}
