package pl.mrugames.nucleus.server.client;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;

class TimeoutClientWorker implements ClientWorker<String, String> {
    CountDownLatch initLatch = new CountDownLatch(1);
    CountDownLatch shutdownLatch = new CountDownLatch(1);
    volatile int amountReceived = 0;

    @Nullable
    @Override
    public String onInit() {
        initLatch.countDown();
        return null;
    }

    @Nullable
    @Override
    public String onRequest(String request) {
        ++amountReceived;
        return null;
    }

    @Nullable
    @Override
    public String onShutdown() {
        shutdownLatch.countDown();
        return null;
    }
}
