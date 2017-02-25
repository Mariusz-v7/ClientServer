package pl.mrugames.commons.client;

import java.util.concurrent.CountDownLatch;

public class SimpleClientWorker implements ClientWorker {
    public static class Factory implements ClientWorkerFactory<String, String> {

        @Override
        public ClientWorker create(String name, Comm<String, String> comm, Runnable shutdownSwitch) {
            return new SimpleClientWorker(comm);
        }
    }

    private final Comm<String, String> comm;
    private final CountDownLatch shutdownSignal = new CountDownLatch(1);

    public SimpleClientWorker(Comm<String, String> comm) {
        this.comm = comm;
    }

    @Override
    public void onClientTermination() {
        shutdownSignal.countDown();
    }

    @Override
    public void run() {

    }

    void waitForShutdown() throws InterruptedException {
        shutdownSignal.await();
    }

    boolean isShutdown() {
        return shutdownSignal.getCount() == 0;
    }

    Comm<String, String> getComm() {
        return comm;
    }
}
