package pl.mrugames.nucleus.server.object_server;

import pl.mrugames.nucleus.server.client.*;

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
