package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

class Client implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final Socket socket;
    private final ExecutorService ioExecutor;
    private final CompletionService<?> completionService;
    private final ClientWriterThread writer;
    private final ClientReaderThread reader;
    private final Runnable onShutdown;

    Client(String name, Socket socket, ClientWriterThread writer, ClientReaderThread reader, Runnable onShutdown) {
        this.name = name;
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.ioExecutor = Executors.newFixedThreadPool(2, this::threadFactory);
        this.onShutdown = onShutdown;
        this.completionService = new ExecutorCompletionService<>(ioExecutor);
    }

    @Override
    public void run() {
        logger.info("[{}] New client has connected from address: {}", name, socket.getLocalSocketAddress());

        try {
            init();

            completionService.take();

            logger.info("[{}] Client is being shutdown", name);  // when woke up when I/O threads finished
        } catch (InterruptedException e) {
            logger.info("[{}] Client is being shutdown", name);  // when woke up by interrupt()
        } finally {
            closeSocket();

            ioExecutor.shutdownNow();

            try {
                ioExecutor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("[{}] Failed to shutdown IO threads!", name);
            }

            onShutdown.run();

            logger.info("[{}] Client has been shutdown!", name);
        }
    }

    CompletableFuture<Object> init() {
        CompletableFuture<Void> writerFuture = CompletableFuture.runAsync(writer, ioExecutor);
        CompletableFuture<Void> readerFuture = CompletableFuture.runAsync(reader, ioExecutor);
        ioExecutor.shutdown();

        return CompletableFuture.anyOf(writerFuture, readerFuture);
    }

    void handleIOThreadException(Thread thread, Throwable exception) {
        closeSocket();
        ioExecutor.shutdownNow();

        if (exception instanceof IOExceptionWrapper) {
            exception = exception.getCause();
        }

        logger.error("[{}][{}] exception in I/O thread, {}", name, thread.getName(), exception.getMessage());
    }

    private Thread threadFactory(Runnable runnable) {
        Thread thread = new Thread(runnable, name + "-I/O");

        thread.setUncaughtExceptionHandler(this::handleIOThreadException);

        return thread;
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            logger.error("[{}] Failed to close socket", name);
        }
    }

    /**
     * This constructor should be used only in tests.
     */
    @Deprecated
    Client(String name, Socket socket, ExecutorService ioExecutor, ClientWriterThread writer, ClientReaderThread reader, Runnable onShutdown, CompletionService<?> completionService) {
        this.name = name;
        this.socket = socket;
        this.ioExecutor = ioExecutor;
        this.reader = reader;
        this.writer = writer;
        this.onShutdown = onShutdown;
        this.completionService = completionService;
    }

}
