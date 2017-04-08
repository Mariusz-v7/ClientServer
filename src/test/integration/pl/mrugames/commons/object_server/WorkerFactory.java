package pl.mrugames.commons.object_server;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;

class WorkerFactory implements ClientWorkerFactory<Frame, Frame> {
    private final Runnable shutdownSwitch;

    WorkerFactory(Runnable shutdownSwitch) {
        this.shutdownSwitch = shutdownSwitch;
    }

    @Override
    public ClientWorker create(String name, Comm<Frame, Frame> comm, Runnable shutdownSwitch) {
        return new Worker(this.shutdownSwitch, name, comm, shutdownSwitch);
    }
}
