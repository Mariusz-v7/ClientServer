package pl.mrugames.commons.object_client;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;
import pl.mrugames.commons.object_server.Frame;

public class WorkerFactory implements ClientWorkerFactory<Frame, Frame> {
    @Override
    public ClientWorker create(String name, Comm<Frame, Frame> comm, Runnable shutdownSwitch) {
        return new Worker(comm, shutdownSwitch);
    }
}
