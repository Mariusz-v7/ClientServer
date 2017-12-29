package pl.mrugames.client_server.client;

import pl.mrugames.client_server.tasks.ClientShutdownTask;

public class KillMe {
    private volatile Client client;

    /**
     * Schedules a task to kill the client... ;)
     */
    public void pleaseDoIt() {
        client.getTaskExecutor().submit(new ClientShutdownTask(client));
    }

    void setClient(Client client) {
        this.client = client;
    }
}
