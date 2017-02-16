package pl.mrugames.commons.client;

public interface ClientWorkerFactory {
    ClientWorker create(String name, Comm comm);
}
