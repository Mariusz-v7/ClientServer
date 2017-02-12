package pl.mrugames.commons.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class Host extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final String name;
    private final int port;
    private final ClientFactory clientFactory;

    private volatile ServerSocket socket;

    public Host(String name, int port, ClientFactory clientFactory) {
        super(name);
        this.name = name;
        this.port = port;
        this.clientFactory = clientFactory;
    }

    @Override
    public void run() {
        logger.info("[Host {}] Host has started!", name);

        try (ServerSocket socket = new ServerSocket(port)) {
            setSocket(socket);

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

    @Override
    public void interrupt() {
        try {
            logger.info("[Host {}] is being shutdown!", name);

            if (socket != null)
                socket.close();

        } catch (IOException e) {
            logger.error("[Host {}] failed to close socket!", name);
        } finally {
            super.interrupt();
        }
    }

    void next(final ServerSocket socket) throws IOException {
        clientFactory.create(socket.accept());
    }

    void setSocket(ServerSocket socket) {
        this.socket = socket;
    }
}
