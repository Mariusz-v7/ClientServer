package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class ClientFactory<In, Out, Reader extends Serializable, Writer extends Serializable> {
    private static final Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    private final AtomicLong clientId;
    private final String factoryName;
    private final String clientNamePrefix;
    private final ClientWorkerFactory<In, Out, Reader, Writer> clientWorkerFactory;
    private final List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;
    private final Function<ByteBuffer, ClientWriter<Writer>> clientWriterFactory;
    private final Function<ByteBuffer, ClientReader<Reader>> clientReaderFactory;
    private final FilterProcessor inputFilterProcessor;
    private final FilterProcessor outputFilterProcessor;
    private final ClientWatchdog watchdog;
    private final int bufferSize;

    private final Timer clientSendMetric;
    private final Timer clientReceiveMetric;

    ClientFactory(String factoryName,
                  String clientNamePrefix,
                  ClientWorkerFactory<In, Out, Reader, Writer> clientWorkerFactory,
                  List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories,
                  Function<ByteBuffer, ClientWriter<Writer>> clientWriterFactory,
                  Function<ByteBuffer, ClientReader<Reader>> clientReaderFactory,
                  FilterProcessor inputFilterProcessor,
                  FilterProcessor outputFilterProcessor,
                  ClientWatchdog clientWatchdog,
                  int bufferSize
    ) {
        this.clientId = new AtomicLong();
        this.factoryName = factoryName;
        this.clientNamePrefix = clientNamePrefix;
        this.clientWorkerFactory = clientWorkerFactory;
        this.initializerFactories = initializerFactories;
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.inputFilterProcessor = inputFilterProcessor;
        this.outputFilterProcessor = outputFilterProcessor;
        this.watchdog = clientWatchdog;
        this.bufferSize = bufferSize;

        this.clientSendMetric = Metrics.getRegistry().timer(name(ClientFactory.class, "client", "send"));
        this.clientReceiveMetric = Metrics.getRegistry().timer(name(ClientFactory.class, "client", "receive"));
    }

    public Client<In, Out, Reader, Writer> create(SocketChannel channel, ExecutorService clientRequestExecutor) throws Exception {
        if (!watchdog.isRunning()) {
            throw new IllegalStateException("Client Watchdog is dead! Cannot accept new connection.");
        }

        try {
            logger.info("[{}] New client is being created!", factoryName);

            String clientName = clientNamePrefix + "-" + clientId.incrementAndGet();
            ClientInfo clientInfo = new ClientInfo(clientName, channel.socket());

            Socket socket = channel.socket();

            List<Initializer> initializers = createInitializers(clientName, socket);

            ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);
            readBuffer.flip();

            ByteBuffer writeBuffer = ByteBuffer.allocate(bufferSize);

            Comm<In, Out, Reader, Writer> comm = createComms(clientName, channel, readBuffer, writeBuffer);

            ClientWorker<In, Out> clientWorker = createWorker(clientName, comm, clientInfo);

            Client<In, Out, Reader, Writer> client = createClient(clientName, clientRequestExecutor, initializers, comm, clientWorker, channel, readBuffer);

            watchdog.register(client);

            logger.info("[{}] New client has been created: {}!", factoryName, client.getName());
            return client;
        } catch (Exception e) {
            closeChannel(channel);
            throw e;
        }
    }

    List<Initializer> createInitializers(String clientName, Socket socket) throws IOException {
        logger.info("[{}] Creating initializers for client: {}", factoryName, clientName);

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        List<Initializer> initializers = initializerFactories.stream()
                .map(factory -> factory.apply(inputStream, outputStream))
                .collect(Collectors.toList());

        logger.info("[{}] {} initializers created for client: {}", factoryName, initializers.size(), clientName);

        return initializers;
    }

    Comm<In, Out, Reader, Writer> createComms(String clientName, SocketChannel channel, ByteBuffer readBuffer, ByteBuffer writeBuffer) throws IOException {
        logger.info("[{}] Creating comms for client: {}", factoryName, clientName);

        ClientWriter<Writer> clientWriter = clientWriterFactory.apply(writeBuffer);
        ClientReader<Reader> clientReader = clientReaderFactory.apply(readBuffer);

        Comm<In, Out, Reader, Writer> comm = new Comm<>(clientWriter,
                clientReader,
                inputFilterProcessor,
                outputFilterProcessor,
                writeBuffer,
                channel,
                clientSendMetric,
                clientReceiveMetric);
        logger.info("[{}] Comms has been created for client: {}", factoryName, clientName);

        return comm;
    }

    ClientWorker<In, Out> createWorker(String clientName, Comm<In, Out, Reader, Writer> comm, ClientInfo clientInfo) {
        logger.info("[{}] Creating client worker for client: {}", factoryName, clientName);

        ClientWorker<In, Out> clientWorker = clientWorkerFactory.create(comm, clientInfo);

        logger.info("[{}] Client worker has been created for client: {}", factoryName, clientName);

        return clientWorker;
    }

    Client<In, Out, Reader, Writer> createClient(String clientName,
                                                 ExecutorService clientsRequestExecutor,
                                                 List<Initializer> initializers,
                                                 Comm<In, Out, Reader, Writer> comm,
                                                 ClientWorker<In, Out> clientWorker,
                                                 SocketChannel channel,
                                                 ByteBuffer readBuffer
    ) {
        return new Client<>(clientName, clientsRequestExecutor, initializers, comm, clientWorker, channel, readBuffer);
    }

    void closeChannel(SocketChannel channel) {
        try {
            logger.error("[{}] Exception during client initialization", factoryName);
            logger.error("[{}] Closing the socket", factoryName);
            channel.close();
            logger.error("[{}] Socket closed", factoryName);
        } catch (IOException e1) {
            logger.error("[{}] Failed to close the socket", factoryName, e1);
        }
    }

}
