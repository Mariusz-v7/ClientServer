package pl.mrugames.client_server.client;

import java.io.Serializable;

public interface ClientWorkerFactory<In, Out, Writer extends Serializable, Reader extends Serializable> {
    Runnable create(Comm<In, Out, Writer, Reader> comm, ClientInfo clientInfo);
}
