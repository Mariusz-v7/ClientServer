package pl.mrugames.nucleus.server.client;

public interface ClientWorkerFactory<In, Out> {
    ClientWorker<In, Out> create(Comm comm, ClientInfo clientInfo, ClientController controller);
}
