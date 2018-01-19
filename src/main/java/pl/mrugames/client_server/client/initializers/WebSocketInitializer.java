package pl.mrugames.client_server.client.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientController;
import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.websocket.WebSocketHandshakeParser;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketInitializer implements ClientWorker<String, String> {
    private final static Logger logger = LoggerFactory.getLogger(WebSocketInitializer.class);

    private final StringBuffer stringBuffer;
    private final WebSocketHandshakeParser parser;
    private final AtomicBoolean isInitialized;
    private final String webSocketProtocolName;
    private final ClientController clientController;
    private final ClientInfo clientInfo;
    private final ClientWorkerFactory<String, String> clientWorkerFactory;

    public WebSocketInitializer(WebSocketHandshakeParser parser,
                                String webSocketProtocolName,
                                ClientController clientController,
                                ClientInfo clientInfo,
                                ClientWorkerFactory<String, String> clientWorkerFactory) {
        this.parser = parser;
        this.stringBuffer = new StringBuffer();
        this.isInitialized = new AtomicBoolean(false);
        this.webSocketProtocolName = webSocketProtocolName;
        this.clientController = clientController;
        this.clientInfo = clientInfo;
        this.clientWorkerFactory = clientWorkerFactory; //todo: create client after initialization
    }

    @Nullable
    @Override
    public String onInit() {
        logger.info("[{}] WebSocket connected. Starting handshake procedure.", clientInfo.getName());
        return null;
    }

    @Nullable
    @Override
    public String onRequest(String request) {
        if (!isInitialized.get()) {
            stringBuffer.append(request);
            stringBuffer.append("\r\n");

            if (parser.isReady(stringBuffer.toString())) {
                logger.info("[{}] WebSocket handshake procedure finished.", clientInfo.getName());

                isInitialized.set(true);
                clientController.switchProtocol(webSocketProtocolName);  // this should be done after send
                return parser.parse(stringBuffer.toString());
            }

            return null;
        }

        return null;
    }

    @Nullable
    @Override
    public String onShutdown() {
        return null;
    }
}
