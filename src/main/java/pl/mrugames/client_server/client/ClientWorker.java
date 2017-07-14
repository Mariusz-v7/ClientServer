package pl.mrugames.client_server.client;

public interface ClientWorker extends Runnable {
    void onClientTermination();
}
