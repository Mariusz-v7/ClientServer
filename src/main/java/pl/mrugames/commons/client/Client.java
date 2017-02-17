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
    private final ClientWriterThread writer;
    private final ClientReaderThread reader;

    Client(String name, Socket socket, ClientWriterThread writer, ClientReaderThread reader) {
        this.name = name;
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.ioExecutor = Executors.newFixedThreadPool(2, r -> new Thread(r, name + "-IO"));
    }

    @Override
    public void run() {
        logger.info("[{}] New client has connected from address: {}", name, socket.getLocalSocketAddress());

        try {
            init().get();
            logger.info("[{}] Client is being shutdown", name);  // when woke up when I/O threads finished
        } catch (InterruptedException e) {
            logger.info("[{}] Client is being shutdown due to interruption", name);  // when woke up by interrupt()
        } catch (ExecutionException e) {
            logger.info("[{}] Client is being shutdown due to exception in the I/O threads, {}", name, e.getMessage());
        } catch (Exception e) {
            logger.info("[{}] Client is being shutdown due to exception, {}, {}", name, e.getClass().getSimpleName(), e.getMessage());
        } finally {
            close();
        }
    }

    private void close() {
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
