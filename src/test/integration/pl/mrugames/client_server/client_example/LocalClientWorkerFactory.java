package pl.mrugames.client_server.client_example;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;

public class LocalClientWorkerFactory implements ClientWorkerFactory<String, String> {
    @Override
    public ClientWorker create(Comm<String, String> comm, Runnable shutdownSwitch, ClientInfo clientInfo) {
        return new LocalClientWorker(comm, shutdownSwitch);
    }
}