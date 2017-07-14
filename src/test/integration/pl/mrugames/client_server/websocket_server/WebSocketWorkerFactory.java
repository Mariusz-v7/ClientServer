package pl.mrugames.client_server.websocket_server;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;

public class WebSocketWorkerFactory implements ClientWorkerFactory<String, String> {
    private final Runnable onShutdownCommand;

    public WebSocketWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(Comm<String, String> comm, Runnable shutdownSwitch, ClientInfo clientInfo) {
        return new Worker(comm, shutdownSwitch, onShutdownCommand);
    }
}
