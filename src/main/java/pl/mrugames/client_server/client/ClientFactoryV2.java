package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.filters.FilterProcessorV2;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientFactoryV2<In, Out, Reader extends Serializable, Writer extends Serializable> {
    private static final Logger logger = LoggerFactory.getLogger(ClientFactoryV2.class);

    private final AtomicLong clientId;
    private final String factoryName;
    private final String clientNamePrefix;
    private final ClientWorkerFactoryV2<In, Out, Reader, Writer> clientWorkerFactory;
    private final List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;
    private final Function<OutputStream, ClientWriter<Writer>> clientWriterFactory;
    private final Function<InputStream, ClientReader<Reader>> clientReaderFactory;
    private final FilterProcessorV2 inputFilterProcessor;
    private final FilterProcessorV2 outputFilterProcessor;
    private final ExecutorService executorService;
    private final ClientWatchdog watchdog;
    final long clientStartTimeoutMilliseconds = 1000;

    ClientFactoryV2(String factoryName,
                    String clientNamePrefix,
                    ClientWorkerFactoryV2<In, Out, Reader, Writer> clientWorkerFactory,
                    List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories,
                    Function<OutputStream, ClientWriter<Writer>> clientWriterFactory,
                    Function<InputStream, ClientReader<Reader>> clientReaderFactory,
                    FilterProcessorV2 inputFilterProcessor,
                    FilterProcessorV2 outputFilterProcessor,
                    ExecutorService executorService,
                    ClientWatchdog clientWatchdog
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
        this.executorService = executorService;
        this.watchdog = clientWatchdog;
    }

    public ClientV2 create(Socket socket) throws Exception {
        if (!watchdog.isRunning()) {
            throw new IllegalStateException("Client Watchdog is dead! Cannot accept new connection.");
        }

        try {
            logger.info("[{}] New client is being created!", factoryName);

            String clientName = clientNamePrefix + "-" + clientId.incrementAndGet();
            ClientInfo clientInfo = new ClientInfo(clientName, socket);

            List<Initializer> initializers = createInitializers(clientName, socket);
            CommV2<In, Out, Reader, Writer> comm = createComms(clientName, socket);

            watchdog.register(comm, socket, clientName);

            Runnable clientWorker = createWorker(clientName, comm, clientInfo);

            ClientV2 client = createClient(clientName, initializers, clientWorker, socket);

            executorService.execute(client);
            boolean result = client.awaitStart(clientStartTimeoutMilliseconds, TimeUnit.MILLISECONDS);
            if (!result) {
                throw new TimeoutException("Failed to start client");
            }

            logger.info("[{}] New client has been created: {}!", factoryName, client.getName());
            return client;
        } catch (Exception e) {
            try {
                logger.error("[{}] Exception during client initialization", factoryName);
                logger.error("[{}] Closing the socket", factoryName);
                socket.close();
                logger.error("[{}] Socket closed", factoryName);
            } catch (IOException e1) {
                logger.error("[{}] Failed to close the socket", factoryName, e1);
            }

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

    CommV2<In, Out, Reader, Writer> createComms(String clientName, Socket socket) throws IOException {
        logger.info("[{}] Creating comms for client: {}", factoryName, clientName);

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        ClientWriter<Writer> clientWriter = clientWriterFactory.apply(outputStream);
        ClientReader<Reader> clientReader = clientReaderFactory.apply(inputStream);

        CommV2<In, Out, Reader, Writer> comm = new CommV2<>(clientWriter, clientReader, inputFilterProcessor, outputFilterProcessor);
        logger.info("[{}] Comms has been created for client: {}", factoryName, clientName);

        return comm;
    }

    Runnable createWorker(String clientName, CommV2<In, Out, Reader, Writer> comm, ClientInfo clientInfo) {
        logger.info("[{}] Creating client worker for client: {}", factoryName, clientName);

        Runnable clientWorker = clientWorkerFactory.create(comm, clientInfo);

        logger.info("[{}] Client worker has been created for client: {}", factoryName, clientName);

        return clientWorker;
    }

    ClientV2 createClient(String clientName, List<Initializer> initializers, Runnable clientWorker, Socket socket) {
        return new ClientV2(clientName, initializers, clientWorker, socket);
    }

}
