package pl.mrugames.client_server.client.helpers;

import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.client_server.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.client_server.client.frames.WebSocketFrame;
import pl.mrugames.client_server.client.initializers.WebSocketInitializer;
import pl.mrugames.client_server.client.io.ObjectReader;
import pl.mrugames.client_server.client.io.ObjectWriter;
import pl.mrugames.client_server.client.io.WebSocketReader;
import pl.mrugames.client_server.client.io.WebSocketWriter;
import pl.mrugames.client_server.websocket.WebSocketHandshakeParser;

import java.io.Serializable;
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

    public static ClientFactory<? extends Serializable, ? extends Serializable, ? extends Serializable, ? extends Serializable> createClientFactoryForJavaServer(
            String name, int timeoutSeconds, ClientWorkerFactory<? extends Serializable, ? extends Serializable> clientWorkerFactory) {

        return new ClientFactoryBuilder<>(ObjectWriter::new, ObjectReader::new, clientWorkerFactory)
                .setClientName(name)
                .setTimeout(timeoutSeconds)
                .build();
    }
}
