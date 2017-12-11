package pl.mrugames.client_server.client_example;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactoryV2;
import pl.mrugames.client_server.client.CommV2;

public class LocalClientWorkerFactory implements ClientWorkerFactoryV2<String, String, String, String> {

    @Override
    public Runnable create(CommV2<String, String, String, String> comm, ClientInfo clientInfo) {
        return new LocalClientWorker(comm);
    }
}
