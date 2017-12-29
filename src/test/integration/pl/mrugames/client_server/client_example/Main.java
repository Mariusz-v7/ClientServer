package pl.mrugames.client_server.client_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientFactoryBuilder;
import pl.mrugames.client_server.client.io.TextReader;
import pl.mrugames.client_server.client.io.TextWriter;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws Exception {
        if (args.length != 2) {
            logger.error("Please provide address and port");
            return;
        }

        logger.info("Main started...");

        final String address = args[0];
        final int port = Integer.valueOf(args[1]);

        ExecutorService executorService = Executors.newCachedThreadPool();

        ClientFactory<String, String, String, String> clientFactory =
                new ClientFactoryBuilder<>(TextWriter::new, TextReader::new, new LocalClientWorkerFactory(), executorService)
                        .setName("Local Client")
                        .build();

        try {
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(address, port));
//            Client localClientWorker = clientFactory.create(socketChannel, executorService);

//            localClientWorker.awaitStop(1, TimeUnit.DAYS);
        } finally {
            executorService.shutdownNow();
        }
    }
}
