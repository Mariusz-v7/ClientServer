package pl.mrugames.commons.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.helpers.ClientFactoryBuilder;
import pl.mrugames.commons.client.io.ObjectReader;
import pl.mrugames.commons.client.io.ObjectWriter;

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

        ClientFactory clientFactory = new ClientFactoryBuilder<>(
                ObjectWriter::new, ObjectReader::new, new WorkerFactory())
                .setClientName("Local Client")
                .build();

        Worker localClientWorker = (Worker) clientFactory.create(new Socket(address, port)).get();

        localClientWorker.getShutdownSignal().await();

        clientFactory.shutdown();
    }
}