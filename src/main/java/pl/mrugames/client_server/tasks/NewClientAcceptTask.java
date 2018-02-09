package pl.mrugames.client_server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ConnectionWatchdog;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

public class NewClientAcceptTask<In, Out> implements Callable<Client<In, Out>> {
    private final static Logger logger = LoggerFactory.getLogger(NewClientAcceptTask.class);

    private final String hostName;
    private final ClientFactory<In, Out> clientFactory;
    private final SocketChannel clientChannel;
    private final TaskExecutor taskExecutor;
    private final ConnectionWatchdog watchdog;

    public NewClientAcceptTask(String hostName,
                               ClientFactory<In, Out> clientFactory,
                               SocketChannel clientChannel,
                               TaskExecutor taskExecutor,
                               ConnectionWatchdog watchdog) {
        this.hostName = hostName;
        this.clientFactory = clientFactory;
        this.clientChannel = clientChannel;
        this.taskExecutor = taskExecutor;
        this.watchdog = watchdog;
    }

    @Override
    public Client<In, Out> call() throws Exception {
        logger.info("[{}] New Client is connecting", hostName);

        Client<In, Out> client = null;

        try {
            logger.info("[{}] New client has been accepted: {}/{}", hostName, clientChannel.getLocalAddress(), clientChannel.getRemoteAddress());

            client = clientFactory.create(clientChannel, taskExecutor, watchdog);

            Out onInitResult = client.getClientWorker().onInit();
            if (onInitResult != null) {
                client.getComm().send(onInitResult);
            }

            return client;
        } catch (Exception e) {
            logger.error("[{}] Error during client creation", hostName, e);

            if (client != null) {
                client.getTaskExecutor().submit(new ClientShutdownTask(client), client.getRequestTimeoutSeconds());
            } else {
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
    void close(SocketChannel socketChannel) throws IOException {
        socketChannel.close();
    }
}
