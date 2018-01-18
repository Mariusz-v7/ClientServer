package pl.mrugames.client_server.client;

import pl.mrugames.client_server.tasks.ClientShutdownTask;

public class ClientController {
    private volatile Client client;

    /**
     * Schedules a task to kill the client... ;)
     */
    public void shutdown() {
        client.getTaskExecutor().submit(new ClientShutdownTask(client));
    }

    public void switchProtocol(Protocol protocol) {
        client.getComm().switchProtocol(protocol.getName());
    }

    public void switchProtocol(String name) {
        client.getComm().switchProtocol(name);
    }

    void setClient(Client client) {
        this.client = client;
    }
}
