package pl.mrugames.client_server.websocket_server;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactoryV2;
import pl.mrugames.client_server.client.CommV2;
import pl.mrugames.client_server.client.frames.WebSocketFrame;

public class WebSocketWorkerFactory implements ClientWorkerFactoryV2<String, String, WebSocketFrame, WebSocketFrame> {
    private final Runnable onShutdownCommand;

    public WebSocketWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public Runnable create(CommV2<String, String, WebSocketFrame, WebSocketFrame> comm, ClientInfo clientInfo) {
        return new Worker(comm, onShutdownCommand);
    }
}
