package pl.mrugames.client_server.object_server;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;

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
