package pl.mrugames.client_server.object_server;

import pl.mrugames.client_server.client.*;

class WorkerFactory implements ClientWorkerFactory<Frame, Frame> {
    private final Runnable onShutDown;

    WorkerFactory(Runnable onShutDown) {
        this.onShutDown = onShutDown;
    }

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new Worker(comm, onShutDown, controller);
    }
}
