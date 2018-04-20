package pl.mrugames.nucleus.server.client.initializers;

import pl.mrugames.nucleus.server.client.*;
import pl.mrugames.nucleus.server.websocket.WebSocketHandshakeParser;

public class WebSocketInitializerFactory implements ClientWorkerFactory<String, String> {
    private final String httpProtocol;
    private final String webSocketProtocol;
    private final ClientWorkerFactory<String, String> clientWorkerFactory;

    public WebSocketInitializerFactory(String httpProtocol, String webSocketProtocol, ClientWorkerFactory<String, String> clientWorkerFactory) {
        this.httpProtocol = httpProtocol;
        this.webSocketProtocol = webSocketProtocol;
        this.clientWorkerFactory = clientWorkerFactory;
    }

    @Override
    public ClientWorker<String, String> create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new WebSocketInitializer(WebSocketHandshakeParser.getInstance(), webSocketProtocol, controller, clientInfo, clientWorkerFactory, comm);
    }
}
