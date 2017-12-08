package pl.mrugames.client_server.object_server;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactoryV2;
import pl.mrugames.client_server.client.CommV2;

class WorkerFactory implements ClientWorkerFactoryV2<Frame, Frame, Frame, Frame> {
    private final Runnable onShutDown;

    WorkerFactory(Runnable onShutDown) {
        this.onShutDown = onShutDown;
    }

    @Override
    public Runnable create(CommV2<Frame, Frame, Frame, Frame> comm, ClientInfo clientInfo) {
        return new Worker(comm, onShutDown);
    }
}
