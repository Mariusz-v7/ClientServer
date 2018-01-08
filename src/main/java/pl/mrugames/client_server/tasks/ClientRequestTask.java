package pl.mrugames.client_server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class ClientRequestTask implements Callable<Void> {
    private final static Logger logger = LoggerFactory.getLogger(ClientRequestTask.class);

    private final Client client;

    public ClientRequestTask(Client client) {
        this.client = client;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void call() throws Exception {

        RequestExecuteTask task;
        try {
            task = executeTask();

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

    RequestExecuteTask executeTask() throws Exception {
        List<Object> requests = new LinkedList<>();

        Object request;
        while (client.getComm().canRead()) {
            request = client.getComm().receive();
            if (request != null) {
                requests.add(request);
            }
        }

        for (int i = 0; i < requests.size(); ++i) {
            if (i < requests.size() - 1) {
                client.getTaskExecutor().submit(new RequestExecuteTask(client, requests.get(i)));
            } else {
                return new RequestExecuteTask(client, requests.get(i));
            }
        }

        return null;
    }

    void executeLastTask(RequestExecuteTask task) throws Exception {
        task.call();
    }
}
