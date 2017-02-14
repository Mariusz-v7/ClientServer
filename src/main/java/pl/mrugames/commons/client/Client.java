package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final Socket socket;
    private final ExecutorService ioExecutor;

    Client(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
        this.ioExecutor = Executors.newFixedThreadPool(2);
    }

    @Override
    public void run() {
        logger.info("[{}] New client has connected from address: {}", name, socket.getLocalSocketAddress());

        try {
            initAndWAit();
        } catch (InterruptedException e) {
            logger.info("[{}] Client is being shutdown", name);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("[{}] Failed to close socket", name);
            }
        }

        logger.info("[{}] Client has been shutdown!", name);
    }

    synchronized void initAndWAit() throws InterruptedException {
        wait();
    }
}
