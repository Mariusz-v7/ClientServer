package pl.mrugames.client_server.object_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Comm;

class Worker implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Comm<Frame, Frame, Frame, Frame> comm;
    private final Runnable onShutDown;

    Worker(Comm<Frame, Frame, Frame, Frame> comm, Runnable onShutDown) {
        this.comm = comm;
        this.onShutDown = onShutDown;
    }

    @Override
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
            onShutDown.run();
        }
    }
}
