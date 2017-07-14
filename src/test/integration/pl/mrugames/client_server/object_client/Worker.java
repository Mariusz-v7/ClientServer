package pl.mrugames.client_server.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.object_server.Frame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

class Worker implements ClientWorker {
    private final static Logger logger = LoggerFactory.getLogger(Worker.class);
    private final Comm<Frame, Frame> comm;
    private final Runnable shutdownSwitch;
    private final CountDownLatch shutdownSignal = new CountDownLatch(1);

    Worker(Comm<Frame, Frame> comm, Runnable shutdownSwitch) {
        this.comm = comm;
        this.shutdownSwitch = shutdownSwitch;
    }

    @Override
    public void onClientTermination() {
        shutdownSwitch.run();
    }

    @Override
    public void run() {
        try {
            Stream.of("First", "Second", "Third").forEach(m -> {
                comm.send(new Frame(m));
                try {
                    logger.info("Frame received: {}", comm.receive(10, TimeUnit.SECONDS));
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
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
}
