package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Client implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final Socket socket;
    private final ExecutorService ioExecutor;
    private final ClientWriter writer;
    private final ClientReader reader;

    Client(String name, Socket socket, ClientWriter writer, ClientReader reader) {
        this.name = name;
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.ioExecutor = Executors.newFixedThreadPool(2);
    }

    @Override
    public void run() {
        logger.info("[{}] New client has connected from address: {}", name, socket.getLocalSocketAddress());

        try {
            init();

            synchronized (this) {
                wait();
            }

            logger.info("[{}] Client is being shutdown", name);  // when woke up by notify()
        } catch (InterruptedException e) {
            logger.info("[{}] Client is being shutdown", name);  // when woke up by interrupt()
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("[{}] Failed to close socket", name);
            }
        }

        ioExecutor.shutdownNow();

        try {
            ioExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("[{}] Failed to shutdown IO threads!");
        }

        logger.info("[{}] Client has been shutdown!", name);
    }

    void init() {
        ioExecutor.execute(writer);
        ioExecutor.execute(reader);
        ioExecutor.shutdown();
    }

    /**
     * This constructor should be used only in tests.
     */
    Client(String name, Socket socket, ExecutorService ioExecutor, ClientWriter writer, ClientReader reader) {
        this.name = name;
        this.socket = socket;
        this.ioExecutor = ioExecutor;
        this.reader = reader;
        this.writer = writer;
    }

}
