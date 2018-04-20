package pl.mrugames.nucleus.server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.server.client.Client;

import java.io.IOException;
import java.util.concurrent.Callable;

@SuppressWarnings("unchecked")
public class ClientShutdownTask implements Callable<Void> {
    private final static Logger logger = LoggerFactory.getLogger(ClientShutdownTask.class);

    private final Client client;

    public ClientShutdownTask(Client client) {
        this.client = client;
    }

    @Override
    public Void call() throws Exception {
        if (client.getShutdown().getAndSet(true)) {
            throw new IllegalStateException("Client was already shutdown.");
        }

        try {
            Object result = client.getClientWorker().onShutdown();
            if (result != null) {
                client.getComm().send(result);
            }
        } finally {
            closeChannel();
        }

        return null;
    }

    /**
     * Mocking purposes
     */
    void closeChannel() {
        try {
            client.getChannel().close();
        } catch (IOException e) {
            logger.error("[{}] Failed to close channel", client.getName(), e);
        }
    }
}
