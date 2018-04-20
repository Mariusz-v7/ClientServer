package pl.mrugames.nucleus.server.websocket_server;

import pl.mrugames.nucleus.server.client.*;

public class WebSocketWorkerFactory implements ClientWorkerFactory<String, String> {
    private final Runnable onShutdownCommand;

    public WebSocketWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new Worker(comm, onShutdownCommand);
    }
}
