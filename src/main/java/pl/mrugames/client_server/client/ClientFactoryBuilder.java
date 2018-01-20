package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.Filter;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class ClientFactoryBuilder<In, Out> {
    private final Function<ByteBuffer, ClientWriter> clientWriterFactory;
    private final Function<ByteBuffer, ClientReader> clientReaderFactory;
    private final ClientWorkerFactory<In, Out> clientWorkerFactory;
    private final ExecutorService executorService;

    private String name = "Client";
    private int timeout = 60;
    private List<Filter<?, ?>> inputFilters = Collections.emptyList();
    private List<Filter<?, ?>> outputFilters = Collections.emptyList();
    private int bufferSize = 1024;

    public ClientFactoryBuilder(Function<ByteBuffer, ClientWriter> clientWriterFactory,
                                Function<ByteBuffer, ClientReader> clientReaderFactory,
                                ClientWorkerFactory<In, Out> clientWorkerFactory,
                                ExecutorService executorService) {  // todo: add separate executor service for maintenance (check ClientFactories for reference)
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.clientWorkerFactory = clientWorkerFactory;
        this.executorService = executorService;
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

    public ClientFactoryBuilder<In, Out> setInputFilters(List<Filter<?, ?>> inputFilters) {
        this.inputFilters = inputFilters;
        return this;
    }

    public ClientFactoryBuilder<In, Out> setOutputFilters(List<Filter<?, ?>> outputFilters) {
        this.outputFilters = outputFilters;
        return this;
    }

    public ClientFactoryBuilder<In, Out> setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    @SuppressWarnings("unchecked")
    public ClientFactory<In, Out> build() {
        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog", timeout);
        executorService.execute(clientWatchdog);

        return new ClientFactory(
                name,
                name + "-client",
                clientWorkerFactory,
                Collections.singletonList(new ProtocolFactory(clientWriterFactory, clientReaderFactory, new FilterProcessor(inputFilters), new FilterProcessor(outputFilters), "")), // TODO: add list, and name
                clientWatchdog,
                bufferSize
        );
    }
}
