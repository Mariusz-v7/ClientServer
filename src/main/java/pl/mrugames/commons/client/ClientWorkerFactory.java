package pl.mrugames.commons.client;

public interface ClientWorkerFactory<In, Out> {
    ClientWorker create(String name, Comm<In, Out> comm, Runnable shutdownSwitch);
}
