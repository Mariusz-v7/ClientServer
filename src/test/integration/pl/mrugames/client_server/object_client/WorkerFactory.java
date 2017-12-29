package pl.mrugames.client_server.object_client;

import pl.mrugames.client_server.client.*;
import pl.mrugames.client_server.object_server.Frame;

public class WorkerFactory implements ClientWorkerFactory<Frame, Frame, Frame, Frame> {

    @Override
    public ClientWorker create(Comm<Frame, Frame, Frame, Frame> comm, ClientInfo clientInfo, KillMe killme) {
        return new Worker(comm);
    }
}
