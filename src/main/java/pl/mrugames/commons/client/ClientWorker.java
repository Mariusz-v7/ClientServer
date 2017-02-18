package pl.mrugames.commons.client;

public interface ClientWorker extends Runnable {
    void onClientTermination();
}
