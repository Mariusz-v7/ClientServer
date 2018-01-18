package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.client_server.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.client_server.client.io.LineReader;
import pl.mrugames.client_server.client.io.LineWriter;
import pl.mrugames.client_server.client.io.WebSocketReader;
import pl.mrugames.client_server.client.io.WebSocketWriter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClientFactories {
    public static ClientFactory<String, String> createClientFactoryForWSServer(
            String name,
            int timeoutSeconds,
            ClientWorkerFactory<String, String> clientWorkerFactory,
            ExecutorService executorService,
            int bufferSize) {

        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog", timeoutSeconds);
        executorService.execute(clientWatchdog);

        List<ProtocolFactory<?, ?>> protocolFactories = new LinkedList<>();
        protocolFactories.add(
                new ProtocolFactory<>(LineWriter::new, LineReader::new, FilterProcessor.EMPTY_FILTER_PROCESSOR, FilterProcessor.EMPTY_FILTER_PROCESSOR, "http-protocol")
        );

        protocolFactories.add(
                new ProtocolFactory<>(WebSocketWriter::new, WebSocketReader::new,
                        FilterProcessor.oneFilterFactory(WebSocketFrameToStringFilter.getInstance()),
                        FilterProcessor.oneFilterFactory(StringToWebSocketFrameFilter.getInstance()),
                        "websocket-protocol")
        );

        // todo: add worker who switch protocol

        return new ClientFactory<>(
                name,
                name + "-client",
                clientWorkerFactory,
                protocolFactories,
                clientWatchdog,
                bufferSize
        );
    }

    public static ClientFactory<?, ?> createClientFactoryForJavaServer(
            String name,
            int timeoutSeconds,
            ClientWorkerFactory<?, ?> clientWorkerFactory,
            ExecutorService executorService,
            int bufferSize
    ) {

        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog", timeoutSeconds);
        executorService.execute(clientWatchdog);

        return null; // todo
//        return new ClientFactory<>(
//                name,
//                name + "-client",
//                clientWorkerFactory,
//                new ProtocolFactory<>(ObjectWriter::new, ObjectReader::new, new FilterProcessor(Collections.emptyList()), new FilterProcessor(Collections.emptyList())),
//                clientWatchdog,
//                bufferSize
//        );
    }
}
