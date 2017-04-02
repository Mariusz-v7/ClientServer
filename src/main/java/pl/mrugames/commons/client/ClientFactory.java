package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.io.ClientReader;
import pl.mrugames.commons.client.io.ClientWriter;

import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ClientFactory<WF, WS, RF, RS> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String clientName;
    private final ExecutorService workerExecutor;
    private final int timeout;
    private final Supplier<ClientWriter<WF, WS>> clientWriterSupplier;
    private final Supplier<ClientReader<RF, RS>> clientReaderSupplier;
    private final ClientWorkerFactory clientWorkerFactory;
    private final AtomicLong id;

    private volatile boolean shutdown;

    public ClientFactory(
            String clientName,
            int maxThreads,
            int timeout,
            Supplier<ClientWriter<WF, WS>> clientWriterSupplier,
            Supplier<ClientReader<RF, RS>> clientReaderSupplier,
            ClientWorkerFactory<RF, WF> clientWorkerFactory) {
        this.clientName = clientName;
        this.workerExecutor = Executors.newFixedThreadPool(maxThreads, this::factory);
        this.timeout = timeout;
        this.clientWriterSupplier = clientWriterSupplier;
        this.clientReaderSupplier = clientReaderSupplier;
        this.clientWorkerFactory = clientWorkerFactory;
        this.id = new AtomicLong();
    }

    public ClientWorker create(Socket socket) {
        if (shutdown) {
            throw new IllegalStateException(String.format("[%s] Factory is shut down, and cannot accept more clients!", clientName));
        }

        try {
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(timeout));

            BlockingQueue<RF> in = new LinkedBlockingQueue<>();
            BlockingQueue<WF> out = new LinkedBlockingQueue<>();

            Comm<RF, WF> comm = new Comm<>(in, out);

            String name = clientName + " " + id.incrementAndGet();

            @SuppressWarnings("unchecked")
            ClientWriterThread writerThread = new ClientWriterThread(name, socket.getOutputStream(), out, clientWriterSupplier.get(), timeout, TimeUnit.SECONDS);
            @SuppressWarnings("unchecked")
            ClientReaderThread readerThread = new ClientReaderThread(name, socket.getInputStream(), in, clientReaderSupplier.get());

            Client client = new Client(workerExecutor, name, socket, writerThread, readerThread);
            @SuppressWarnings("unchecked")
            ClientWorker clientWorker = clientWorkerFactory.create(name, comm, client::shutdown);
            if (clientWorker == null) {
                throw new NullPointerException("Client worker is null");
            }

            workerExecutor.submit(clientWorker);
            client.run().whenCompleteAsync((v, t) -> clientWorker.onClientTermination());

            return clientWorker;
        } catch (Exception e) {
            logger.error("[{}] Failed to initialize client, {}", clientName, e.getMessage());
            return null;
        }
    }

    private Thread factory(Runnable runnable) {
        Thread thread = new Thread(runnable, clientName + " - factory");
        thread.setUncaughtExceptionHandler((t, e) -> {
            logger.error("[{}] Error in client thread, {}", t.getName(), e.getMessage());
            t.interrupt();
        });
        return thread;
    }

    public void shutdown() {
        logger.info("[{}] Factory is being shutdown!", clientName);

        shutdown = true;

        workerExecutor.shutdownNow();

        try {
            logger.info("[{}] Shutting down worker threads!", clientName);
            Thread.interrupted(); /// clear the flag
            boolean result = workerExecutor.awaitTermination(30, TimeUnit.SECONDS);
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
