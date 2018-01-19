package pl.mrugames.client_server.client_example;

import pl.mrugames.client_server.client.*;

public class LocalClientWorkerFactory implements ClientWorkerFactory<String, String> {

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new LocalClientWorker(comm);
    }
}
