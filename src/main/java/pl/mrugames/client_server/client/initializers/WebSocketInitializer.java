package pl.mrugames.client_server.client.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.*;
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
    private final Comm comm;

    private volatile ClientWorker<String, String> targetWorker;

    WebSocketInitializer(WebSocketHandshakeParser parser,
                         String webSocketProtocolName,
                         ClientController clientController,
                         ClientInfo clientInfo,
                         ClientWorkerFactory<String, String> clientWorkerFactory,
                         Comm comm) {
        this.parser = parser;
        this.stringBuffer = new StringBuffer();
        this.isInitialized = new AtomicBoolean(false);
        this.webSocketProtocolName = webSocketProtocolName;
        this.clientController = clientController;
        this.clientInfo = clientInfo;
        this.clientWorkerFactory = clientWorkerFactory;
        this.comm = comm;
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
        synchronized (this) {
            if (!isInitialized.get()) {
                stringBuffer.append(request);

                if (parser.isReady(stringBuffer.toString())) {
                    isInitialized.set(true);
                    targetWorker = clientWorkerFactory.create(comm, clientInfo, clientController);

                    comm.switchProtocol(webSocketProtocolName);

                    try {
                        comm.send(parser.parse(stringBuffer.toString()));
                    } catch (Exception e) {
                        //todo
                    }

                    logger.info("[{}] WebSocket handshake procedure finished.", clientInfo.getName());

                    return targetWorker.onInit();
                }

                return null;
            }
        }

        return targetWorker.onRequest(request);
    }

    @Nullable
    @Override
    public String onShutdown() {
        if (targetWorker != null) {
            return targetWorker.onShutdown();
        }

        return null;
    }
}
