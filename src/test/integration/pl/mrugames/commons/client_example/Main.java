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
        logger.info("Main started...");

        ClientFactory clientFactory = new ClientFactory<>(
                "Local Client",
                1,
                60,
                TextClientWriter::getInstance,
                TextClientReader::getInstance,
                new LocalClientWorkerFactory()
        );

        LocalClientWorker localClientWorker = (LocalClientWorker) clientFactory.create(new Socket("localhost", 10000));

        localClientWorker.getShutdownLatch().await();

        clientFactory.shutdown();
    }
}
