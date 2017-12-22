package pl.mrugames.client_server.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactories;
import pl.mrugames.client_server.client.ClientFactory;

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

        ClientFactory clientFactory = ClientFactories.createClientFactoryForJavaServer("Local Client", 60, new WorkerFactory(), executorService);

        try {
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(address, port));
            Client client = clientFactory.create(socketChannel, executorService);

//            client.awaitStop(1, TimeUnit.DAYS);
        } finally {
            executorService.shutdownNow();
        }
    }
}
