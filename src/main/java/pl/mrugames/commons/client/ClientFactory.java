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
    private final ExecutorService clientExecutor;
    private final ExecutorService workerExecutor;
    private final int timeout;
    private final Supplier<ClientWriter<WF, WS>> clientWriterSupplier;
    private final Supplier<ClientReader<RF, RS>> clientReaderSupplier;
    private final ClientWorkerFactory clientWorkerFactory;
    private final AtomicLong id;

    public ClientFactory(
            String clientName,
            int maxThreads,
            int timeout,
            Supplier<ClientWriter<WF, WS>> clientWriterSupplier,
            Supplier<ClientReader<RF, RS>> clientReaderSupplier,
            ClientWorkerFactory clientWorkerFactory) {
        this.clientName = clientName;
        this.clientExecutor = Executors.newFixedThreadPool(maxThreads, this::factory);
        this.workerExecutor = Executors.newFixedThreadPool(maxThreads, this::factory);
        this.timeout = timeout;
        this.clientWriterSupplier = clientWriterSupplier;
        this.clientReaderSupplier = clientReaderSupplier;
        this.clientWorkerFactory = clientWorkerFactory;
        this.id = new AtomicLong();
    }

    public void create(Socket socket) {
        BlockingQueue<RF> in = new LinkedBlockingQueue<>();
        BlockingQueue<WF> out = new LinkedBlockingQueue<>();

        Comm<RF, WF> comm = new Comm<>(in, out);

        String name = clientName + " " + id.incrementAndGet();

        Client client = null;
        ClientWorker clientWorker = null;
        try {
            socket.setSoTimeout(timeout);

            @SuppressWarnings("unchecked")
            ClientWriterThread writerThread = new ClientWriterThread(socket.getOutputStream(), out, clientWriterSupplier.get(), timeout, TimeUnit.SECONDS);
            @SuppressWarnings("unchecked")
            ClientReaderThread readerThread = new ClientReaderThread(socket.getInputStream(), in, clientReaderSupplier.get());

            clientWorker = clientWorkerFactory.create(name, comm);
            client = new Client(name, socket, writerThread, readerThread, clientWorker::onClientDown);

        } catch (Exception e) {
            logger.error("[{}] Failed to initialize client, {}", clientName, e.getMessage());
        }

        if (clientWorker != null && client != null) {
            clientExecutor.submit(client);
            workerExecutor.submit(clientWorker);
        } else {
            logger.error("[{}] Failed to initialize client or worker.", clientName);
        }
    }

    private Thread factory(Runnable runnable) {
        Thread thread = new Thread(runnable, clientName + " - pool");
        thread.setUncaughtExceptionHandler((t, e) -> {
            logger.error("[{}] Error in client thread, {}", t.getName(), e.getMessage());
            t.interrupt();
        });
        return thread;
    }

}
