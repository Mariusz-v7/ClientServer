package pl.mrugames.client_server.host.tasks;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.codahale.metrics.MetricRegistry.name;

public class NewClientAcceptTask<In, Out, Reader extends Serializable, Writer extends Serializable> implements Callable<Client<In, Out, Reader, Writer>> {
    private final static Logger logger = LoggerFactory.getLogger(NewClientAcceptTask.class);

    private final String hostName;
    private final ClientFactory<In, Out, Reader, Writer> clientFactory;
    private final SocketChannel clientChannel;
    private final ExecutorService clientRequestExecutor;
    private final Timer clientAcceptMetric;

    public NewClientAcceptTask(String hostName,
                               ClientFactory<In, Out, Reader, Writer> clientFactory,
                               SocketChannel clientChannel,
                               ExecutorService clientRequestExecutor) {
        this.hostName = hostName;
        this.clientFactory = clientFactory;
        this.clientChannel = clientChannel;
        this.clientRequestExecutor = clientRequestExecutor;
        clientAcceptMetric = Metrics.getRegistry().timer(name(NewClientAcceptTask.class, hostName));
    }

    @Override
    public Client<In, Out, Reader, Writer> call() throws Exception {
        logger.info("[{}] New Client is connecting", hostName);

        try (Timer.Context ignored = clientAcceptMetric.time()) {
            logger.info("[{}] New client has been accepted: {}/{}", hostName, clientChannel.getLocalAddress(), clientChannel.getRemoteAddress());

            Client<In, Out, Reader, Writer> client = clientFactory.create(clientChannel, clientRequestExecutor);

            Out onInitResult = client.getClientWorker().onInit();
            if (onInitResult != null) {
                client.getComm().send(onInitResult);
            }

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
    void close(SocketChannel socketChannel) throws IOException {
        socketChannel.close();
    }
}
