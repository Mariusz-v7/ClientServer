package pl.mrugames.nucleus.server.object_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.server.client.ClientController;
import pl.mrugames.nucleus.server.client.ClientWorker;
import pl.mrugames.nucleus.server.client.Comm;

import javax.annotation.Nullable;

class Worker implements ClientWorker<Frame, Frame> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Comm comm;
    private final Runnable shutdownServer;
    private final ClientController clientController;

    Worker(Comm comm, Runnable shutdownServer, ClientController clientController) {
        this.comm = comm;
        this.shutdownServer = shutdownServer;
        this.clientController = clientController;
    }

    @Override
    public Frame onInit() {
        logger.info("Client initialized");
        return null;
    }

    @Override
    public Frame onRequest(Frame request) {
        logger.info("Frame received: {}", request);

        if (request.getMessage().equals("shutdown")) {
            logger.info("Initiating shutdown");
            shutdownServer.run();
        }

        return new Frame("your message was: " + request.getMessage());
    }

    @Nullable
    @Override
    public Frame onShutdown() {
        logger.info("Client shutdown");
        return null;
    }
}
