package pl.mrugames.client_server.object_client;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactoryV2;
import pl.mrugames.client_server.client.CommV2;
import pl.mrugames.client_server.object_server.Frame;

public class WorkerFactory implements ClientWorkerFactoryV2<Frame, Frame, Frame, Frame> {

    @Override
    public Runnable create(CommV2<Frame, Frame, Frame, Frame> comm, ClientInfo clientInfo) {
        return new Worker(comm);
    }
}
