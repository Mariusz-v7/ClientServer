package pl.mrugames.client_server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;

import java.util.concurrent.Callable;

public class ClientRequestTask implements Callable<Void> {
    private final static Logger logger = LoggerFactory.getLogger(ClientRequestTask.class);

    //    private final List<Initializer> initializers; // TODO: initializers
    private final Client client;


    public ClientRequestTask(Client client) {
        this.client = client;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void call() throws Exception {
        RequestExecuteTask task = null;

        try {
            boolean canRead = client.getComm().canRead();
            while (canRead) {
                Object request = client.getComm().receive();
                if (request == null) {
                    return null;
                }

                canRead = client.getComm().canRead();

                if (canRead) {
                    client.getTaskExecutor().submit(new RequestExecuteTask(client, request));
                } else {
                    task = new RequestExecuteTask(client, request);
                }
            }
        } catch (Exception e) {
            logger.error("[{}] Failed to process request", client.getName(), e);

            client.getTaskExecutor().submit(new ClientShutdownTask(client));
            throw e;
        }

        if (task != null) {
            executeLastTask(task);
        }

        return null;
    }

    void executeLastTask(RequestExecuteTask task) throws Exception {
        task.call();
    }
}
