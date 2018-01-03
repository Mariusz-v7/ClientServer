package pl.mrugames.client_server.object_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.client.KillMe;

import javax.annotation.Nullable;

class Worker implements ClientWorker<Frame, Frame> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Comm<Frame, Frame, Frame, Frame> comm;
    private final Runnable shutdownServer;
    private final KillMe killMe;

    Worker(Comm<Frame, Frame, Frame, Frame> comm, Runnable shutdownServer, KillMe killMe) {
        this.comm = comm;
        this.shutdownServer = shutdownServer;
        this.killMe = killMe;
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
