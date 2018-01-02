package pl.mrugames.client_server.tasks;

import pl.mrugames.client_server.client.Client;

import java.util.concurrent.Callable;

public class RequestExecuteTask implements Callable<Void> {
    private final Client client;
    private final Object frame;

    public RequestExecuteTask(Client client, Object frame) {
        this.client = client;
        this.frame = frame;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void call() throws Exception {
        try {
            Object response = client.getClientWorker().onRequest(frame);
            if (response != null) {
                client.getComm().send(response);
            }
        } catch (Exception e) {
            client.getTaskExecutor().submit(new ClientShutdownTask(client));
            throw e;
        }

        return null;
    }

    Object getFrame() {
        return frame;
    }
}
