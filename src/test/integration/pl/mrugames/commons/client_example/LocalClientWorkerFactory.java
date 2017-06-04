package pl.mrugames.commons.client_example;

import pl.mrugames.commons.client.ClientInfo;
import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;

public class LocalClientWorkerFactory implements ClientWorkerFactory<String, String> {
    @Override
    public ClientWorker create(Comm<String, String> comm, Runnable shutdownSwitch, ClientInfo clientInfo) {
        return new LocalClientWorker(comm, shutdownSwitch);
    }
}
