package pl.mrugames.commons.client_example;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;

public class LocalClientWorkerFactory implements ClientWorkerFactory<String, String> {
    @Override
    public ClientWorker create(String name, Comm<String, String> comm, Runnable shutdownSwitch) {
        return new LocalClientWorker(comm, shutdownSwitch);
    }
}
