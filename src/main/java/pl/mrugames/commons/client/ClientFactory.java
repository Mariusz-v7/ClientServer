package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.initializers.Initializer;
import pl.mrugames.commons.client.io.ClientReader;
import pl.mrugames.commons.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientFactory<WF, RF> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String clientName;
    private final ExecutorService threadPool;
    private final int timeout;
    private final Function<OutputStream, ClientWriter<WF>> clientWriterFactory;
    private final Function<InputStream, ClientReader<RF>> clientReaderFactory;
    private final ClientWorkerFactory clientWorkerFactory;
    private final AtomicLong id;
    private final List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;

    private volatile boolean shutdown;

    public ClientFactory(
            String clientName,
            int timeout,
            Function<OutputStream, ClientWriter<WF>> clientWriterFactory,
            Function<InputStream, ClientReader<RF>> clientReaderFactory,
            ClientWorkerFactory<RF, WF> clientWorkerFactory,
            List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories) {
        this.clientName = clientName;
        this.threadPool = Executors.newCachedThreadPool(this::factory);
        this.timeout = timeout;
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.clientWorkerFactory = clientWorkerFactory;
        this.initializerFactories = initializerFactories;
        this.id = new AtomicLong();
    }

    public CompletableFuture<ClientWorker> create(Socket socket) {
        if (shutdown) {
            throw new IllegalStateException(String.format("[%s] Factory is shut down, and cannot accept more clients!", clientName));
        }

        try {
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(timeout));

            return initialize(socket).thenApply(o -> {
                try {
                    return initWorker(socket);
                } catch (IOException e) {
                    throw new IOExceptionWrapper(e);
                }
            });
        } catch (Exception e) {
            logger.error("[{}] Failed to initialize client, {}", clientName, e.getMessage());
            return null;
        }
    }

    private ClientWorker initWorker(Socket socket) throws IOException {
        BlockingQueue<RF> in = new LinkedBlockingQueue<>();
        BlockingQueue<WF> out = new LinkedBlockingQueue<>();

        Comm<RF, WF> comm = new Comm<>(in, out);

        String name = clientName + " " + id.incrementAndGet();

        @SuppressWarnings("unchecked")
        ClientWriterThread writerThread = new ClientWriterThread(name, out, clientWriterFactory.apply(socket.getOutputStream()), timeout, TimeUnit.SECONDS);
        @SuppressWarnings("unchecked")
        ClientReaderThread readerThread = new ClientReaderThread(name, in, clientReaderFactory.apply(socket.getInputStream()));

        Client client = new Client(threadPool, name, socket, writerThread, readerThread);
        @SuppressWarnings("unchecked")
        ClientWorker clientWorker = clientWorkerFactory.create(name, comm, client::close);
        if (clientWorker == null) {
            throw new NullPointerException("Client worker is null");
        }

        threadPool.submit(clientWorker);
        client.start()
                .whenCompleteAsync((v, t) -> clientWorker.onClientTermination(), threadPool);

        return clientWorker;
    }

    private CompletableFuture<Void> initialize(Socket socket) throws IOException {
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
