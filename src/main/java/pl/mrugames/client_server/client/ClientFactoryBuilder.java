package pl.mrugames.client_server.client;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ClientFactoryBuilder<In, Out> {
    private final ClientWorkerFactory<In, Out> clientWorkerFactory;
    private final ExecutorService maintenanceExecutor;
    private final List<ProtocolFactory> protocolFactories;

    private String name = "Client";
    private int timeout = 60;
    private int bufferSize = 1024;

    public ClientFactoryBuilder(ClientWorkerFactory<In, Out> clientWorkerFactory,
                                ExecutorService maintenanceExecutor,
                                ProtocolFactory initialProtocolFactory) {
        this.clientWorkerFactory = clientWorkerFactory;
        this.maintenanceExecutor = maintenanceExecutor;
        this.protocolFactories = new LinkedList<>();
        this.protocolFactories.add(initialProtocolFactory);
    }

    public ClientFactoryBuilder<In, Out> setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param timeout [s]
     */
    public ClientFactoryBuilder<In, Out> setTimeout(int timeout) {
        this.timeout = timeout;
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
        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog");  // TODO: why cannot have common watchdog?
        maintenanceExecutor.execute(clientWatchdog);

        return new ClientFactory(
                name,
                name + "-client",
                clientWorkerFactory,
                protocolFactories,
                clientWatchdog,
                bufferSize,
                timeout
        );
    }
}
