package pl.mrugames.client_server.client;

import java.util.LinkedList;
import java.util.List;

public class ClientFactoryBuilder<In, Out> {
    private final ClientWorkerFactory<In, Out> clientWorkerFactory;
    private final List<ProtocolFactory> protocolFactories;

    private String name = "Client";
    private int connectionTimeout = 60;
    private int requestTimeout = 30;
    private int bufferSize = 1024;

    public ClientFactoryBuilder(ClientWorkerFactory<In, Out> clientWorkerFactory,
                                ProtocolFactory initialProtocolFactory) {
        this.clientWorkerFactory = clientWorkerFactory;
        this.protocolFactories = new LinkedList<>();
        this.protocolFactories.add(initialProtocolFactory);
    }

    public ClientFactoryBuilder<In, Out> setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param connectionTimeout [s]
     */
    public ClientFactoryBuilder<In, Out> setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public ClientFactoryBuilder<In, Out> setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public ClientFactoryBuilder<In, Out> setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public ClientFactoryBuilder<In, Out> addProtocolFactory(ProtocolFactory protocolFactory) {
        this.protocolFactories.add(protocolFactory);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ClientFactory<In, Out> build() {
        return new ClientFactory(
                name,
                name + "-client",
                clientWorkerFactory,
                protocolFactories,
                bufferSize,
                connectionTimeout,
                requestTimeout
        );
    }
}
