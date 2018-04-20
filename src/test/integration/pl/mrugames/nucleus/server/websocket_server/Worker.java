package pl.mrugames.nucleus.server.websocket_server;

import pl.mrugames.nucleus.server.client.ClientWorker;
import pl.mrugames.nucleus.server.client.Comm;

import javax.annotation.Nullable;

public class Worker implements ClientWorker {
    private final Comm comm;
    private final Runnable onClientShutDown;

    public Worker(Comm comm, Runnable onClientShutDown) {
        this.comm = comm;
        this.onClientShutDown = onClientShutDown;
    }

    @Override
    public Object onInit() {
        return null;
    }

    @Override
    public Object onRequest(Object request) {
        return request;
    }

    @Nullable
    @Override
    public Object onShutdown() {
        return null;
    }
}
