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

    @Deprecated
    public void run() {
        Frame frame;

        try {
            do {
                frame = comm.receive();
                logger.info("Frame received: {}", frame);
                comm.send(new Frame("your message was: " + frame.getMessage()));
            } while (!Thread.currentThread().isInterrupted() && !frame.getMessage().equals("shutdown"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutdownServer.run();
        }
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
        return null;
    }
}
