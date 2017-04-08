package pl.mrugames.commons.client.helpers;

import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.commons.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.commons.client.frames.WebSocketFrame;
import pl.mrugames.commons.client.initializers.WebSocketInitializer;
import pl.mrugames.commons.client.io.WebSocketReader;
import pl.mrugames.commons.client.io.WebSocketWriter;
import pl.mrugames.commons.websocket.WebSocketHandshakeParser;

import java.util.Collections;

public class ClientFactories {
    public static ClientFactory<WebSocketFrame, WebSocketFrame, String, String> createClientFactoryForWSServer(
            String name, int timeoutSeconds, ClientWorkerFactory<String, String> clientWorkerFactory) {
        return new ClientFactory<>(
                name,
                timeoutSeconds,
                WebSocketWriter::new,
                WebSocketReader::new,
                clientWorkerFactory,
                Collections.singletonList(WebSocketInitializer.create(WebSocketHandshakeParser.getInstance())),
                Collections.singletonList(WebSocketFrameToStringFilter.getInstance()),
                Collections.singletonList(StringToWebSocketFrameFilter.getInstance())
        );
    }
}
