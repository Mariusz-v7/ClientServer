package pl.mrugames.client_server.client;

import java.io.Serializable;

public interface ClientWorkerFactory<In, Out, Writer extends Serializable, Reader extends Serializable> {
    ClientWorker<In, Out> create(Comm comm, ClientInfo clientInfo, ClientController killme);
}
