package pl.mrugames.client_server.object_server;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;

class WorkerFactory implements ClientWorkerFactory<Frame, Frame, Frame, Frame> {
    private final Runnable onShutDown;

    WorkerFactory(Runnable onShutDown) {
        this.onShutDown = onShutDown;
    }

    @Override
    public Runnable create(Comm<Frame, Frame, Frame, Frame> comm, ClientInfo clientInfo) {
        return new Worker(comm, onShutDown);
    }
}
