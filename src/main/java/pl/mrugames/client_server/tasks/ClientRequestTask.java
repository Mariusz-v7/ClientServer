package pl.mrugames.client_server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.initializers.Initializer;

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

        boolean result = runInitializers();
        if (!result) {
            return null;
        }

        RequestExecuteTask task = executeTask();

        if (task != null) {
            executeLastTask(task);
        }

        return null;
    }

    RequestExecuteTask executeTask() throws Exception {
        //todo: several race conditions on comm#canRead -> comm#read. Whole comm (or even client) should be locked during read.

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

        return task;
    }

    void executeLastTask(RequestExecuteTask task) throws Exception {
        task.call();
    }

    boolean runInitializers() throws Exception {
        List<Initializer> initializers = client.getInitializers();
        for (Initializer initializer : initializers) {
            //TODO: whole initializer should be locked
            // TODO: when exception then shutdown client
            if (initializer.isCompleted()) {
                continue;
            }

            if (!initializer.getComm().canRead()) {
                return false;
            }

            Object frame = initializer.getComm().receive();
            if (frame == null) {
                return false;
            }

            Object result = initializer.proceed(frame);
            if (result != null) {
                initializer.getComm().send(result);
            }

            if (!initializer.isCompleted()) {
                return false;
            }
        }

        logger.info("[{}] All initializers are done!", client.getName());
        initializers.clear();

        return true;
    }
}
