package pl.mrugames.commons.client_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.helpers.ClientFactoryBuilder;
import pl.mrugames.commons.client.io.TextReader;
import pl.mrugames.commons.client.io.TextWriter;

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

        ClientFactory<String, String, String, String> clientFactory =
                new ClientFactoryBuilder<>(TextWriter::new, TextReader::new, new LocalClientWorkerFactory())
                        .setClientName("Local Client")
                        .build();

        LocalClientWorker localClientWorker = (LocalClientWorker) clientFactory.create(new Socket(address, port)).get();

        localClientWorker.getShutdownLatch().await();

        clientFactory.shutdown();
    }
}
