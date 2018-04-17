package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.initializers.WebSocketInitializerFactory;

import java.io.Serializable;
import java.util.List;

public class ClientFactories {

    public static ClientFactory<String, String> createClientFactoryForWSServer(
            String name,
            int connectionTimeoutSeconds,
            int requestTimeoutSeconds,
            ClientWorkerFactory<String, String> clientWorkerFactory,
            int bufferSize) {

        String httpProtocolName = "http-protocol";
        String webSocketProtocolName = "web-socket-protocol";

        return new ClientFactory<>(
                name,
                name + "-client",
                new WebSocketInitializerFactory(httpProtocolName, webSocketProtocolName, clientWorkerFactory),
                ProtocolFactories.createProtocolFactoryForWebSocket(httpProtocolName, webSocketProtocolName),
                bufferSize,
                connectionTimeoutSeconds,
                requestTimeoutSeconds
        );
    }

    public static <In, Out> ClientFactory<In, Out> createClientFactoryForJavaServer(
            String name,
            int connectionTimeoutSeconds,
            int requestTimeoutSeconds,
            ClientWorkerFactory<In, Out> clientWorkerFactory,
            int bufferSize,
            List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories
    ) {
        return new ClientFactory<>(
                name,
                name + "-client",
                clientWorkerFactory,
                protocolFactories,
                bufferSize,
                connectionTimeoutSeconds,
                requestTimeoutSeconds
        );
    }
}
