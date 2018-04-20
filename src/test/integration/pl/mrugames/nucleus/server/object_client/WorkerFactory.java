package pl.mrugames.nucleus.server.object_client;

import pl.mrugames.nucleus.server.client.*;
import pl.mrugames.nucleus.server.object_server.Frame;

public class WorkerFactory implements ClientWorkerFactory<Frame, Frame> {

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new Worker(comm);
    }
}
