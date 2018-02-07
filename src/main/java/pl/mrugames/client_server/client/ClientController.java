package pl.mrugames.client_server.client;

import pl.mrugames.client_server.tasks.ClientShutdownTask;

public class ClientController {
    private volatile Client client;

    /**
     * Schedules a task to kill the client.
     */
    public void shutdown() {
        client.getTaskExecutor().submit(new ClientShutdownTask(client), client.getRequestTimeoutSeconds());
    }

    public void switchProtocol(Protocol protocol, SwitchProtocolStrategy switchProtocolStrategy) {
        client.scheduleProtocolSwitch(new ProtocolSwitch(protocol.getName(), switchProtocolStrategy));
    }

    public void switchProtocol(String name, SwitchProtocolStrategy switchProtocolStrategy) {
        client.scheduleProtocolSwitch(new ProtocolSwitch(name, switchProtocolStrategy));
    }

    void setClient(Client client) {
        this.client = client;
    }
}
