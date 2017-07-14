package pl.mrugames.client_server.object_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.util.concurrent.TimeUnit;

class Worker implements ClientWorker {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Runnable shutdownSwitch;
    private final Comm<Frame, Frame> comm;
    private final Runnable onClientShutDown;

    Worker(Runnable shutdownSwitch, Comm<Frame, Frame> comm, Runnable onClientShutDown) {
        this.shutdownSwitch = shutdownSwitch;
        this.comm = comm;
        this.onClientShutDown = onClientShutDown;
    }

    @Override
    public void onClientTermination() {
        shutdownSwitch.run();
    }

    @Override
    public void run() {
        Frame frame;

        try {
            do {
                frame = comm.receive(60, TimeUnit.SECONDS);
                logger.info("Frame received: {}", frame);
                comm.send(new Frame("your message was: " + frame.getMessage()));
            } while (!Thread.currentThread().isInterrupted() && !frame.getMessage().equals("shutdown"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
