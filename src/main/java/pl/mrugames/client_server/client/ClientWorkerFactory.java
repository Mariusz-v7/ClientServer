package pl.mrugames.client_server.client;

public interface ClientWorkerFactory<In, Out> {
    ClientWorker<In, Out> create(Comm comm, ClientInfo clientInfo, ClientController killme);
}
