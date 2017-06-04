package pl.mrugames.commons.client;

public interface ClientWorkerFactory<In, Out> {
    ClientWorker create(Comm<In, Out> comm, Runnable shutdownSwitch, ClientInfo clientInfo);
}
