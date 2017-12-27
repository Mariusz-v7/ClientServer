package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.Filter;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientFactoryBuilder<In, Out, Reader extends Serializable, Writer extends Serializable> {
    private final Function<ByteBuffer, ClientWriter<Writer>> clientWriterFactory;
    private final Function<ByteBuffer, ClientReader<Reader>> clientReaderFactory;
    private final ClientWorkerFactory<In, Out, Reader, Writer> clientWorkerFactory;
    private final ExecutorService executorService;

    private String name = "Client";
    private int timeout = 60;
    private List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories = Collections.emptyList();
    private List<Filter<?, ?>> inputFilters = Collections.emptyList();
    private List<Filter<?, ?>> outputFilters = Collections.emptyList();

    public ClientFactoryBuilder(Function<ByteBuffer, ClientWriter<Writer>> clientWriterFactory,
                                Function<ByteBuffer, ClientReader<Reader>> clientReaderFactory,
                                ClientWorkerFactory<In, Out, Reader, Writer> clientWorkerFactory,
                                ExecutorService executorService) {
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.clientWorkerFactory = clientWorkerFactory;
        this.executorService = executorService;
    }

    public ClientFactoryBuilder<In, Out, Reader, Writer> setName(String name) {
        this.name = name;
        return this;
    }

    public ClientFactoryBuilder<In, Out, Reader, Writer> setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ClientFactoryBuilder<In, Out, Reader, Writer> setInitializerFactories(List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories) {
        this.initializerFactories = initializerFactories;
        return this;
    }

    public ClientFactoryBuilder<In, Out, Reader, Writer> setInputFilters(List<Filter<?, ?>> inputFilters) {
        this.inputFilters = inputFilters;
        return this;
    }

    public ClientFactoryBuilder<In, Out, Reader, Writer> setOutputFilters(List<Filter<?, ?>> outputFilters) {
        this.outputFilters = outputFilters;
        return this;
    }

    public ClientFactory<In, Out, Reader, Writer> build() {
        ClientWatchdog clientWatchdog = new ClientWatchdog(name + "-watchdog", timeout);
        executorService.execute(clientWatchdog);

        return new ClientFactory<>(
                name,
                name + "-client",
                clientWorkerFactory,
                initializerFactories,
                null, null,//todo
//                clientWriterFactory,
//                clientReaderFactory,
                new FilterProcessor(inputFilters),
                new FilterProcessor(outputFilters),
                clientWatchdog
        );
    }
}
