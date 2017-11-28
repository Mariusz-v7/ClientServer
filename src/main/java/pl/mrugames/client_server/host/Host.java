package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class Host extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final int port;
    private final ClientFactory clientFactory;
    private final CompletableFuture<Boolean> startResult;

    private volatile ServerSocket socket;

    Host(String name, int port, ClientFactory clientFactory) {
        super(name);
        this.port = port;
        this.clientFactory = clientFactory;
        this.startResult = new CompletableFuture<>();
    }

    @Override
    public void run() {
        logger.info("[Host {}] Host has started! Listening on port: {}!", getName(), port);

        try (ServerSocket socket = new ServerSocket(port)) {
            setSocket(socket);
            startResult.complete(true);

            while (!isInterrupted()) {
                try {
                    next(socket);
                } catch (Exception e) {
                    logger.error("[Host {}] Failed to initialize client", getName());

                    if (socket.isClosed()) {
                        logger.error("[Host {}] ServerSocket is closed", getName());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("[Host {}] Failed to create socket: {}", getName(), e.getMessage());
        } finally {
            clientFactory.shutdown();

            if (!startResult.isDone()) {
                startResult.complete(false);
            }
        }

        logger.info("[Host {}] Host has been shutdown!", getName());
    }

    @Override
    public void interrupt() {
        logger.info("[Host {}] is being shutdown!", getName());

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("[Host {}] failed to close socket!", getName());
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

    boolean waitForSocketOpen() throws InterruptedException {
        try {
            return startResult.get();
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }
}
