package pl.mrugames.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.io.TextClientReader;
import pl.mrugames.commons.client.io.TextClientWriter;
import pl.mrugames.commons.host.Host;

public class Example {
    private final static Logger logger = LoggerFactory.getLogger(Example.class);
    private static Host host;

    public static void main(String ...args) throws InterruptedException {
        logger.info("Example started...");


        ClientFactory clientFactory = new ClientFactory<>(
                "Example Client",
                20,
                60,
                TextClientWriter::getInstance,
                TextClientReader::getInstance,
                new ExampleClientWorkerFactory(Example::shutdown)
        );

        host = new Host("Example Host", 10000, clientFactory);

        host.start();
        host.join();

        logger.info("Example finished...");
    }

    private static void shutdown() {
        host.interrupt();
    }
}
