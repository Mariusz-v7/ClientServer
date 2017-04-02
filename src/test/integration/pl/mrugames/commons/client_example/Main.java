package pl.mrugames.commons.client_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.io.TextClientReader;
import pl.mrugames.commons.client.io.TextClientWriter;

import java.io.IOException;
import java.net.Socket;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String ...args) throws InterruptedException, IOException {
        if (args.length != 2) {
            logger.error("Please provide address and port");
            return;
        }

        logger.info("Main started...");

        final String address = args[0];
        final int port = Integer.valueOf(args[1]);

        ClientFactory clientFactory = new ClientFactory<>(
                "Local Client",
                60,
                TextClientWriter::getInstance,
                TextClientReader::getInstance,
                new LocalClientWorkerFactory()
        );

        LocalClientWorker localClientWorker = (LocalClientWorker) clientFactory.create(new Socket(address, port));

        localClientWorker.getShutdownLatch().await();

        clientFactory.shutdown();
    }
}
