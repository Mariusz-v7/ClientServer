package pl.mrugames.commons.object_server;

import pl.mrugames.commons.client.ClientInfo;
import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;

class WorkerFactory implements ClientWorkerFactory<Frame, Frame> {
    private final Runnable shutdownSwitch;

    WorkerFactory(Runnable shutdownSwitch) {
        this.shutdownSwitch = shutdownSwitch;
    }

    @Override
    public ClientWorker create(Comm<Frame, Frame> comm, Runnable shutdownSwitch, ClientInfo clientInfo) {
        return new Worker(this.shutdownSwitch, comm, shutdownSwitch);
    }
}
