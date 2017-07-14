package pl.mrugames.client_server.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.filters.Filter;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientFactory<WorldIn extends Serializable, WorldOut extends Serializable, ClientIn, ClientOut> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String clientName;
    private final ExecutorService threadPool;
    private final int timeout;
    private final Function<OutputStream, ClientWriter<WorldOut>> clientWriterFactory;
    private final Function<InputStream, ClientReader<WorldIn>> clientReaderFactory;
    private final ClientWorkerFactory<ClientIn, ClientOut> clientWorkerFactory;
    private final AtomicLong id;
    private final List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;
    private final List<Filter<?, ?>> inputFilters;
    private final List<Filter<?, ?>> outputFilters;
    private final Counter activeWorkers;
    private final Counter activeReaders;
    private final Counter activeWriters;

    private volatile boolean shutdown;

    public ClientFactory(
            String clientName,
            int timeout,
            Function<OutputStream, ClientWriter<WorldOut>> clientWriterFactory,
            Function<InputStream, ClientReader<WorldIn>> clientReaderFactory,
            ClientWorkerFactory<ClientIn, ClientOut> clientWorkerFactory,
            List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories,
            List<Filter<?, ?>> inputFilters,
            List<Filter<?, ?>> outputFilters) {
        this.clientName = clientName;
        this.threadPool = Executors.newCachedThreadPool(this::factory);
        this.timeout = timeout;
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.clientWorkerFactory = clientWorkerFactory;
        this.initializerFactories = initializerFactories;
        this.inputFilters = inputFilters;
        this.outputFilters = outputFilters;
        this.id = new AtomicLong();
        this.activeWorkers = HealthCheckManager.getMetricRegistry().counter(MetricRegistry.name(ClientFactory.class, clientName, "active_workers"));
        this.activeWriters = HealthCheckManager.getMetricRegistry().counter(MetricRegistry.name(ClientFactory.class, clientName, "active_writers"));
        this.activeReaders = HealthCheckManager.getMetricRegistry().counter(MetricRegistry.name(ClientFactory.class, clientName, "active_readers"));

        if (threadPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
            MetricRegistry metricRegistry = HealthCheckManager.getMetricRegistry();
            metricRegistry.register(MetricRegistry.name(ClientFactory.class, clientName, "pool_active_count"), (Gauge) tpe::getActiveCount);
            metricRegistry.register(MetricRegistry.name(ClientFactory.class, clientName, "pool_size"), (Gauge) tpe::getPoolSize);
            metricRegistry.register(MetricRegistry.name(ClientFactory.class, clientName, "largest_pool_size"), (Gauge) tpe::getLargestPoolSize);
        }
    }

    public CompletableFuture<ClientWorker> create(Socket socket) {
        if (shutdown) {
            throw new IllegalStateException(String.format("[%s] Factory is shut down, and cannot accept more clients!", clientName));
        }

        try {
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(timeout));

            return initialize(socket)
                    .thenApply(o -> initWorker(socket))
                    .exceptionally(e -> {
                        logger.error("[{}] Failed to initialize client", clientName, e);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("[{}] Failed to initialize client", clientName, e);
            return null;
        }
    }

    ClientWorker initWorker(Socket socket) {
        try {
            BlockingQueue<ClientIn> in = new LinkedBlockingQueue<>();
            BlockingQueue<ClientOut> out = new LinkedBlockingQueue<>();

            Comm<ClientIn, ClientOut> comm = new Comm<>(in, out);

            String name = clientName + " " + id.incrementAndGet();

            ClientWriterThread<ClientOut, WorldOut> writerThread = new ClientWriterThread<>(name, out,
                    clientWriterFactory.apply(socket.getOutputStream()),
                    timeout, TimeUnit.SECONDS,
                    outputFilters, FilterProcessor.getInstance(),
                    activeWriters);

            ClientReaderThread<WorldIn, ClientIn> readerThread = new ClientReaderThread<>(name, in,
                    clientReaderFactory.apply(socket.getInputStream()),
                    inputFilters, FilterProcessor.getInstance(),
                    activeReaders
            );

            Client client = new Client(threadPool, name, socket, writerThread, readerThread);

            ClientWorker clientWorker = clientWorkerFactory.create(comm, client::close, new ClientInfo(name, socket));
            if (clientWorker == null) {
                throw new NullPointerException("Client worker is null");
            }

            threadPool.submit(clientWorker);
            activeWorkers.inc();

            client.start()
                    .thenRunAsync(clientWorker::onClientTermination, threadPool)
                    .thenRunAsync(activeWorkers::dec, threadPool);

            return clientWorker;
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    }

    CompletableFuture<Void> initialize(Socket socket) throws IOException {
        CompletableFuture<Void> initializerFuture = null;
        for (BiFunction<InputStream, OutputStream, Initializer> initializerFactory : initializerFactories) {
            Initializer initializer = initializerFactory.apply(socket.getInputStream(), socket.getOutputStream());

            if (initializerFuture == null) {
                initializerFuture = CompletableFuture.runAsync(initializer, threadPool);
            } else {
                initializerFuture = initializerFuture.thenRun(initializer);
            }
        }

        if (initializerFuture == null) {
            return CompletableFuture.completedFuture(null);
        }

        return initializerFuture;
    }

    private Thread factory(Runnable runnable) {
        Thread thread = new Thread(runnable, "client-factory-pool-" + clientName);
        thread.setUncaughtExceptionHandler((t, e) -> {
            logger.error("[{}] Error in client thread, {}", t.getName(), e.getMessage());
            t.interrupt();
        });
        return thread;
    }

    public void shutdown() {
        logger.info("[{}] Factory is being shutdown!", clientName);

        shutdown = true;

        threadPool.shutdownNow();

        try {
            logger.info("[{}] Shutting down thread pool!", clientName);
            Thread.interrupted(); /// clear the flag
            boolean result = threadPool.awaitTermination(30, TimeUnit.SECONDS);
            if (result) {
                logger.info("[{}] Worker threads has been shutdown!", clientName);
            } else {
                logger.error("[{}] Failed to shutdown worker threads", clientName);
            }
        } catch (InterruptedException e) {
            logger.error("[{}] Failed to shutdown worker threads due to interruption", clientName);
        }

        logger.info("[{}] Factory has been shutdown!", clientName);
    }

}
