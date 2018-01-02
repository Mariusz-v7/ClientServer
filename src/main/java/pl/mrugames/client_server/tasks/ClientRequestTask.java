package pl.mrugames.client_server.tasks;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.Client;

import java.util.concurrent.Callable;

import static com.codahale.metrics.MetricRegistry.name;

public class ClientRequestTask implements Callable<Void> {
    private final static Logger logger = LoggerFactory.getLogger(ClientRequestTask.class);

    //    private final List<Initializer> initializers; // TODO: initializers
    private final Client client;

    private final Timer requestProcessingMetric;

    public ClientRequestTask(Client client) {
        this.client = client;
        requestProcessingMetric = Metrics.getRegistry().timer(name(ClientRequestTask.class, client.getName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void call() throws Exception {
        try {
            while (client.getComm().canRead()) {
                try (Timer.Context ignored = requestProcessingMetric.time()) {
                    Object request = client.getComm().receive();
                    if (request == null) {
                        return null;
                    }

                    client.getTaskExecutor().submit(new RequestExecuteTask(client, request));
                }
            }
        } catch (Exception e) {
            logger.error("[{}] Failed to process request", client.getName(), e);

            client.getTaskExecutor().submit(new ClientShutdownTask(client));
            throw e;
        }

        return null;
    }
}
