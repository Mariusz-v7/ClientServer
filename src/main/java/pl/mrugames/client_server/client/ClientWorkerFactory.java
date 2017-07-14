package pl.mrugames.client_server.client;

public interface ClientWorkerFactory<In, Out> {
    ClientWorker create(Comm<In, Out> comm, Runnable shutdownSwitch, ClientInfo clientInfo);
}
