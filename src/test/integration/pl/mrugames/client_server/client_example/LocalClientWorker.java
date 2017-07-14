package pl.mrugames.client_server.client_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.util.concurrent.CountDownLatch;

public class LocalClientWorker implements ClientWorker {
    private final static Logger logger = LoggerFactory.getLogger(LocalClientWorker.class);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final Comm<String, String> comm;
    private final Runnable shutdownSwitch;

    public LocalClientWorker(Comm<String, String> comm, Runnable shutdownSwitch) {
        this.comm = comm;
        this.shutdownSwitch = shutdownSwitch;
    }

    @Override
    public void onClientTermination() {
        shutdownSwitch.run();
    }

    @Override
    public void run() {
        logger.info("Local client started");

        try {
            for (int i = 0; i < 5; ++i) {
                Thread.sleep(2000);
                logger.info("Sending command: {}", i);
                comm.send("Command: " + i + "\n");
            }
        } catch (Exception e) {
            logger.error("Exception", e);
        }

        logger.info("Sending shutdown command");
        comm.send("shutdown\n");

        shutdownSwitch.run();
        shutdownLatch.countDown();

        logger.info("Local client shutdown");
    }

    public CountDownLatch getShutdownLatch() {
        return shutdownLatch;
    }
}
