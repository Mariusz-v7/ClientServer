package pl.mrugames.commons.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class Host implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final String name;
    private final int port;

    public Host(String name, int port) {
        this.name = name;
        this.port = port;
    }

    @Override
    public void run() {
        logger.info("[Host {}] Host has started!", name);

        try (ServerSocket socket = new ServerSocket(port)) {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    next(socket);
                } catch (Exception e) {
                    logger.error("[Host {}] Failed to initialize client", name);
                }
            }
        } catch (IOException e) {
            logger.error("[Host {}] Failed to create socket: {}", name, e.getMessage());
        }

        logger.info("[Host {}] Host has shutdown!", name);
    }

    void next(final ServerSocket socket) throws IOException {
        socket.accept();
    }
}
