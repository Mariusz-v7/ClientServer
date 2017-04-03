package pl.mrugames.commons.websocket_server;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.Comm;
import pl.mrugames.commons.client.frames.WebSocketFrame;

public class Worker implements ClientWorker {
    private final Runnable shutdownSwitch;
    private final String name;
    private final Comm<WebSocketFrame, WebSocketFrame> comm;
    private final Runnable onClientShutDown;

    public Worker(String name, Comm<WebSocketFrame, WebSocketFrame> comm, Runnable onClientShutDown, Runnable shutdownSwitch) {
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
