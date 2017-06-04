package pl.mrugames.commons.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

public class Host extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final int port;
    private final ClientFactory clientFactory;
    private final CountDownLatch socketOpenSignal = new CountDownLatch(1);

    private volatile ServerSocket socket;

    public Host(String name, int port, ClientFactory clientFactory) {
        super(name);
        this.port = port;
        this.clientFactory = clientFactory;
    }

    @Override
    public void run() {
        logger.info("[Host {}] Host has started!", getName());

        try (ServerSocket socket = new ServerSocket(port)) {
            setSocket(socket);
            socketOpenSignal.countDown();

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

    public void waitForSocketOpen() throws InterruptedException {
        socketOpenSignal.await();
    }

    public void interruptAndJoin() throws InterruptedException {
        interrupt();
        join();
    }
}
