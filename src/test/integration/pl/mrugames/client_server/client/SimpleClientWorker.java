package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class SimpleClientWorker implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static class Factory implements ClientWorkerFactoryV2<String, String, String, String> {
        @Override
        public Runnable create(CommV2<String, String, String, String> comm, ClientInfo clientInfo) {
            return new SimpleClientWorker(comm);
        }
    }

    private final CommV2<String, String, String, String> comm;
    final CountDownLatch shutdownSignal = new CountDownLatch(1);

    public SimpleClientWorker(CommV2<String, String, String, String> comm) {
        this.comm = comm;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                comm.receive();
            }
        } catch (Exception e) {
            logger.info("Client exception:", e);
        }

        shutdownSignal.countDown();
    }

    void waitForShutdown() throws InterruptedException {
        shutdownSignal.await();
    }

    boolean isShutdown() {
        return shutdownSignal.getCount() == 0;
    }

    CommV2<String, String, String, String> getComm() {
        return comm;
    }
}
