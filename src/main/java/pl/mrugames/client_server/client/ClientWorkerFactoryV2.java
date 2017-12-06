package pl.mrugames.client_server.client;

import java.io.Serializable;

public interface ClientWorkerFactoryV2<In, Out, Writer extends Serializable, Reader extends Serializable> {
    Runnable create(CommV2<In, Out, Writer, Reader> comm, ClientInfo clientInfo);
}
