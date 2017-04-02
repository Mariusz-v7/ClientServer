package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

class Client {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final Socket socket;
    private final ExecutorService executorService;
    private final ClientWriterThread writer;
    private final ClientReaderThread reader;

    private volatile boolean closed;

    Client(ExecutorService executorService, String name, Socket socket, ClientWriterThread writer, ClientReaderThread reader) {
        this.name = name;
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.executorService = executorService;
    }

    CompletableFuture<Object> start() {
        logger.info("[{}] New client has connected from address: {}", name, socket.getLocalSocketAddress());

        return init()
                .whenComplete(this::onComplete)
                .exceptionally(e -> null);
    }

    void onComplete(Object value, Throwable throwable) {
        if (throwable != null) {
            if (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }

            if (throwable instanceof IOExceptionWrapper) {
                throwable = throwable.getCause();
            }

            logger.error("[{}] Exception in client execution, {}, {}", name, throwable.getClass(), throwable.getMessage(), throwable);
        }

        close();
    }

    void close() {
        if (closed) {
            return;  // avoid multiple calls
        }

        closed = true;

        reader.interrupt();
        writer.interrupt();

        try {
            logger.info("[{}] Closing socket", name);
            socket.close();
            logger.info("[{}] Socket has been closed", name);
        } catch (IOException e) {
            logger.error("[{}] Failed to close socket", name);
        }

        Thread.interrupted();  // clear the flag
        try {
            logger.info("[{}] Waiting for reader and writer to shutdown", name);
            reader.join();
            writer.join();
            logger.info("[{}] Reader and writer has been shutdown", name);
        } catch (InterruptedException e) {
            logger.warn("[{}] Interrupted during waiting for reader and writer", name);
        }

        logger.info("[{}] Client has been shutdown!", name);
    }

    CompletableFuture<Object> init() {
        CompletableFuture<Void> writerFuture = CompletableFuture.runAsync(writer, executorService);
        CompletableFuture<Void> readerFuture = CompletableFuture.runAsync(reader, executorService);

        return CompletableFuture.anyOf(writerFuture, readerFuture);
    }

}
