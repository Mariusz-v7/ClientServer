package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.client_server.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.client_server.client.initializers.WebSocketInitializerFactory;
import pl.mrugames.client_server.client.io.TextReader;
import pl.mrugames.client_server.client.io.TextWriter;
import pl.mrugames.client_server.client.io.WebSocketReader;
import pl.mrugames.client_server.client.io.WebSocketWriter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClientFactories {
    /**
     * @param maintenanceThreadPool - thread pool for maintenance processes - should be different than pool for client execution
     */
    public static ClientFactory<String, String> createClientFactoryForWSServer(
            String name,
            int timeoutSeconds,
            ClientWorkerFactory<String, String> clientWorkerFactory,
            ExecutorService maintenanceThreadPool,
            int bufferSize) {

        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog"); // todo
        maintenanceThreadPool.execute(clientWatchdog);

        String httpProtocolName = "http-protocol";
        String webSocketProtocolName = "web-socket-protocol";

        List<ProtocolFactory<?, ?>> protocolFactories = new LinkedList<>();
        protocolFactories.add(
                new ProtocolFactory<>(TextWriter::new, TextReader::new, FilterProcessor.EMPTY_FILTER_PROCESSOR, FilterProcessor.EMPTY_FILTER_PROCESSOR, httpProtocolName)
        );

        protocolFactories.add(
                new ProtocolFactory<>(WebSocketWriter::new, WebSocketReader::new,
                        FilterProcessor.oneFilterFactory(WebSocketFrameToStringFilter.getInstance()),
                        FilterProcessor.oneFilterFactory(StringToWebSocketFrameFilter.getInstance()),
                        webSocketProtocolName)
        );

        ClientWorkerFactory<String, String> webSocketWorkerFactory = new WebSocketInitializerFactory(httpProtocolName, webSocketProtocolName, clientWorkerFactory);

        return new ClientFactory<>(
                name,
                name + "-client",
                webSocketWorkerFactory,
                protocolFactories,
                clientWatchdog,
                bufferSize,
                timeoutSeconds
        );
    }

    public static ClientFactory<?, ?> createClientFactoryForJavaServer(
            String name,
            int timeoutSeconds,
            ClientWorkerFactory<?, ?> clientWorkerFactory,
            ExecutorService executorService,
            int bufferSize
    ) {

        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog"); // TODO
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
