package pl.mrugames.commons.websocket_server;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.Comm;

public class Worker implements ClientWorker {
    private final Runnable shutdownSwitch;
    private final String name;
    private final Comm<String, String> comm;
    private final Runnable onClientShutDown;

    public Worker(String name, Comm<String, String> comm, Runnable onClientShutDown, Runnable shutdownSwitch) {
        this.shutdownSwitch = shutdownSwitch;
        this.name = name;
        this.comm = comm;
        this.onClientShutDown = onClientShutDown;
    }

    @Override
    public void onClientTermination() {

    }

    @Override
    public void run() {

    }
}