package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.client_server.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.client_server.client.frames.WebSocketFrame;
import pl.mrugames.client_server.client.initializers.WebSocketInitializer;
import pl.mrugames.client_server.websocket.WebSocketHandshakeParser;

import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

public class ClientFactories {
    public static ClientFactory<String, String, WebSocketFrame, WebSocketFrame> createClientFactoryForWSServer(
            String name,
            int timeoutSeconds,
            ClientWorkerFactory<String, String, WebSocketFrame, WebSocketFrame> clientWorkerFactory,
            ExecutorService executorService,
            int bufferSize) {

        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog", timeoutSeconds);
        executorService.execute(clientWatchdog);

        return new ClientFactory<>(
                name,
                name + "-client",
                clientWorkerFactory,
                Collections.singletonList(WebSocketInitializer.create(WebSocketHandshakeParser.getInstance())),
                null,
                null,
// WebSocketWriter::new, // TODO
//                WebSocketReader::new,
                new FilterProcessor(Collections.singletonList(WebSocketFrameToStringFilter.getInstance())),
                new FilterProcessor(Collections.singletonList(StringToWebSocketFrameFilter.getInstance())),
                clientWatchdog,
                bufferSize
        );
    }

    public static ClientFactory<?, ?, ? extends Serializable, ? extends Serializable> createClientFactoryForJavaServer(
            String name,
            int timeoutSeconds,
            ClientWorkerFactory<?, ?, ? extends Serializable, ? extends Serializable> clientWorkerFactory,
            ExecutorService executorService
    ) {

        return null; //TODO
//        return new ClientFactoryBuilder<>(ObjectWriter::new, ObjectReader::new, clientWorkerFactory, executorService)
//                .setName(name)
//                .setTimeout(timeoutSeconds)
//                .build();
    }
}
