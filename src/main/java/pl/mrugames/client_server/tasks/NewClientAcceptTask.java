package pl.mrugames.client_server.tasks;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientWatchdog;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

import static com.codahale.metrics.MetricRegistry.name;

public class NewClientAcceptTask<In, Out> implements Callable<Client<In, Out>> {
    private final static Logger logger = LoggerFactory.getLogger(NewClientAcceptTask.class);

    private final String hostName;
    private final ClientFactory<In, Out> clientFactory;
    private final SocketChannel clientChannel;
    private final TaskExecutor taskExecutor;
    private final Timer clientAcceptMetric;
    private final ClientWatchdog watchdog;

    public NewClientAcceptTask(String hostName,
                               ClientFactory<In, Out> clientFactory,
                               SocketChannel clientChannel,
                               TaskExecutor taskExecutor,
                               ClientWatchdog watchdog) {
        this.hostName = hostName;
        this.clientFactory = clientFactory;
        this.clientChannel = clientChannel;
        this.taskExecutor = taskExecutor;
        this.watchdog = watchdog;
        clientAcceptMetric = Metrics.getRegistry().timer(name(NewClientAcceptTask.class, hostName));
    }

    @Override
    public Client<In, Out> call() throws Exception {
        logger.info("[{}] New Client is connecting", hostName);

        Client<In, Out> client = null;

        try (Timer.Context ignored = clientAcceptMetric.time()) {
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
                client.getTaskExecutor().submit(new ClientShutdownTask(client));
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
