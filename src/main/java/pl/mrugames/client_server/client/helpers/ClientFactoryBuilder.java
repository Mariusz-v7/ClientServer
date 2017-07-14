package pl.mrugames.client_server.client.helpers;

import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.filters.Filter;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientFactoryBuilder<WriterFrame extends Serializable, ReaderFrame extends Serializable, ClientReadFrame, ClientWriteFrame> {
    private final Function<OutputStream, ClientWriter<WriterFrame>> clientWriterFactory;
    private final Function<InputStream, ClientReader<ReaderFrame>> clientReaderFactory;
    private final ClientWorkerFactory<ClientReadFrame, ClientWriteFrame> clientWorkerFactory;

    private String clientName = "Client";
    private int timeout = 60;
    private List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories = Collections.emptyList();
    private List<Filter<?, ?>> inputFilters = Collections.emptyList();
    private List<Filter<?, ?>> outputFilters = Collections.emptyList();

    public ClientFactoryBuilder(Function<OutputStream, ClientWriter<WriterFrame>> clientWriterFactory,
                                Function<InputStream, ClientReader<ReaderFrame>> clientReaderFactory,
                                ClientWorkerFactory<ClientReadFrame, ClientWriteFrame> clientWorkerFactory) {
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.clientWorkerFactory = clientWorkerFactory;
    }

    public ClientFactoryBuilder<WriterFrame, ReaderFrame, ClientReadFrame, ClientWriteFrame> setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public ClientFactoryBuilder<WriterFrame, ReaderFrame, ClientReadFrame, ClientWriteFrame> setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ClientFactoryBuilder<WriterFrame, ReaderFrame, ClientReadFrame, ClientWriteFrame> setInitializerFactories(List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories) {
        this.initializerFactories = initializerFactories;
        return this;
    }

    public ClientFactoryBuilder<WriterFrame, ReaderFrame, ClientReadFrame, ClientWriteFrame> setInputFilters(List<Filter<?, ?>> inputFilters) {
        this.inputFilters = inputFilters;
        return this;
    }

    public ClientFactoryBuilder<WriterFrame, ReaderFrame, ClientReadFrame, ClientWriteFrame> setOutputFilters(List<Filter<?, ?>> outputFilters) {
        this.outputFilters = outputFilters;
        return this;
    }

    public ClientFactory<ReaderFrame, WriterFrame, ClientReadFrame, ClientWriteFrame> build() {
        return new ClientFactory<>(clientName, timeout, clientWriterFactory,
                clientReaderFactory, clientWorkerFactory, initializerFactories, inputFilters, outputFilters);
    }
}
