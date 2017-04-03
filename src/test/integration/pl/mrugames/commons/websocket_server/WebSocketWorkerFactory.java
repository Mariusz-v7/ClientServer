package pl.mrugames.commons.websocket_server;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;
import pl.mrugames.commons.client.frames.WebSocketFrame;

public class WebSocketWorkerFactory implements ClientWorkerFactory<WebSocketFrame, WebSocketFrame> {
    private final Runnable onShutdownCommand;

    public WebSocketWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(String name, Comm<WebSocketFrame, WebSocketFrame> comm, Runnable shutdownSwitch) {
        return new Worker(name, comm, shutdownSwitch, onShutdownCommand);
    }
}
