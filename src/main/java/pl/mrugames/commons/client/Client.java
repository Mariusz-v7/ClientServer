package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Client {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final Socket socket;
    private final ExecutorService ioExecutor;
    private final ClientWriterThread writer;
    private final ClientReaderThread reader;

    Client(String name, Socket socket, ClientWriterThread writer, ClientReaderThread reader) {
        this.name = name;
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.ioExecutor = Executors.newFixedThreadPool(2, r -> new Thread(r, name + "-IO"));
    }

    CompletableFuture<Object> run() {
        logger.info("[{}] New client has connected from address: {}", name, socket.getLocalSocketAddress());

        return init().whenComplete(this::onComplete).exceptionally(e -> null);
    }

    void onComplete(Object value, Throwable throwable) {
        if (throwable != null) {
            logger.error("[{}] Exception in client execution, {}", name, throwable.getMessage());
        }

        close();
    }

    void close() {
        ioExecutor.shutdownNow();

        try {
            socket.close();
        } catch (IOException e) {
            logger.error("[{}] Failed to close socket", name);
        }

        try {
            ioExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("[{}] Failed to shutdown IO threads!", name);
        }

        logger.info("[{}] Client has been shutdown!", name);
    }

    CompletableFuture<Object> init() {
        CompletableFuture<Void> writerFuture = CompletableFuture.runAsync(writer, ioExecutor);
        CompletableFuture<Void> readerFuture = CompletableFuture.runAsync(reader, ioExecutor);
        ioExecutor.shutdown();

        return CompletableFuture.anyOf(writerFuture, readerFuture);
    }

    /**
     * This constructor should be used only in tests.
     */
    @Deprecated
    Client(String name, Socket socket, ExecutorService ioExecutor, ClientWriterThread writer, ClientReaderThread reader) {
        this.name = name;
        this.socket = socket;
        this.ioExecutor = ioExecutor;
        this.reader = reader;
        this.writer = writer;
    }

}
