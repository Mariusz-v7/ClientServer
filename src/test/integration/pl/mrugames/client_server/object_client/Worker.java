package pl.mrugames.client_server.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.object_server.Frame;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

class Worker implements ClientWorker {
    private final static Logger logger = LoggerFactory.getLogger(Worker.class);
    private final Comm<Frame, Frame, Frame, Frame> comm;
    private final CountDownLatch shutdownSignal = new CountDownLatch(1);

    Worker(Comm<Frame, Frame, Frame, Frame> comm) {
        this.comm = comm;
    }

    @Deprecated
    public void run() {
        try {
            Stream.of("First", "Second", "Third").forEach(m -> {
                try {
                    comm.send(new Frame(m));
                    logger.info("Frame received: {}", comm.receive());
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            comm.send(new Frame("shutdown"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutdownSignal.countDown();
        }
    }

    public CountDownLatch getShutdownSignal() {
        return shutdownSignal;
    }

    @Override
    public Object onRequest(Object request) {
        //TODO
        return null;
    }
}
