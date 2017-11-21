package pl.mrugames.client_server.object_client;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.object_server.Frame;

public class WorkerFactory implements ClientWorkerFactory<Frame, Frame> {
    @Override
    public ClientWorker create(Comm<Frame, Frame> comm, Runnable shutdownSwitch, ClientInfo clientInfo) {
        return new Worker(comm, shutdownSwitch);
    }
}