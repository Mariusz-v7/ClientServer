package pl.mrugames.client_server.host.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.*;
import java.util.concurrent.Callable;

public class NewClientAcceptTask<In, Out, Reader extends Serializable, Writer extends Serializable> implements Callable<Client<In, Out, Reader, Writer>> {
    private final static Logger logger = LoggerFactory.getLogger(NewClientAcceptTask.class);

    private final String hostName;
    private final ClientFactory<In, Out, Reader, Writer> clientFactory;
    private final ServerSocketChannel channel;
    private final Selector selector;

    public NewClientAcceptTask(String hostName, ClientFactory<In, Out, Reader, Writer> clientFactory, ServerSocketChannel channel, Selector selector) {
        this.hostName = hostName;
        this.clientFactory = clientFactory;
        this.channel = channel;
        this.selector = selector;
    }

    @Override
    public Client<In, Out, Reader, Writer> call() throws Exception {
        logger.info("[{}] New Client is connecting", hostName);

        SocketChannel clientChannel = null;
        try {
            clientChannel = channel.accept();

            logger.info("[{}] New client has been accepted: {}/{}", hostName, clientChannel.getLocalAddress(), clientChannel.getRemoteAddress());

            configure(clientChannel);

            Client<In, Out, Reader, Writer> client = clientFactory.create(clientChannel);

            register(clientChannel, client);

            return client;
        } catch (Exception e) {
            logger.error("[{}] Error during client creation", hostName, e);

            if (clientChannel != null) {
                logger.error("[{}] Closing client's socket", hostName);

                try {
                    close(clientChannel);
                    logger.error("[{}] Client's socket closed", hostName);
                } catch (IOException e1) {
                    logger.error("[{}] Failed to close client's socket", hostName, e1);
                }
            }

            throw e;
        }
    }

    /**
     * Mocking purposes
     */
    void register(SocketChannel clientChannel, Client<In, Out, Reader, Writer> client) throws ClosedChannelException {
        clientChannel.register(selector, SelectionKey.OP_READ, client);
    }

    /**
     * Configure client socket.
     * Method for mocking purposes (final methods);
     */
    void configure(SocketChannel clientChannel) throws IOException {
        clientChannel.configureBlocking(false);
    }

    /**
     * Mocking purposes
     */
    void close(SocketChannel socketChannel) throws IOException {
        socketChannel.close();
    }
}
