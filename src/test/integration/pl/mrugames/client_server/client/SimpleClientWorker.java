package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;

public class SimpleClientWorker implements ClientWorker {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Object onInit() {
        //TODO
        return null;
    }

    @Override
    public Object onRequest(Object request) {
        // TODO
        return null;
    }

    @Nullable
    @Override
    public Object onShutdown() {
        //todo
        return null;
    }

    public static class Factory implements ClientWorkerFactory<String, String, String, String> {
        @Override
        public ClientWorker create(Comm<String, String, String, String> comm, ClientInfo clientInfo) {
            return new SimpleClientWorker(comm);
        }
    }

    private final Comm<String, String, String, String> comm;
    final CountDownLatch shutdownSignal = new CountDownLatch(1);

    public SimpleClientWorker(Comm<String, String, String, String> comm) {
        this.comm = comm;
    }

    @Deprecated
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

    Comm<String, String, String, String> getComm() {
        return comm;
    }
}
