package pl.mrugames.client_server.websocket_server;

import pl.mrugames.client_server.client.*;
import pl.mrugames.client_server.client.frames.WebSocketFrame;

public class WebSocketWorkerFactory implements ClientWorkerFactory<String, String, WebSocketFrame, WebSocketFrame> {
    private final Runnable onShutdownCommand;

    public WebSocketWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController killme) {
        return new Worker(comm, onShutdownCommand);
    }
}
