package pl.mrugames.client_server.object_client;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.object_server.Frame;

public class WorkerFactory implements ClientWorkerFactory<Frame, Frame, Frame, Frame> {

    @Override
    public Runnable create(Comm<Frame, Frame, Frame, Frame> comm, ClientInfo clientInfo) {
        return new Worker(comm);
    }
}
