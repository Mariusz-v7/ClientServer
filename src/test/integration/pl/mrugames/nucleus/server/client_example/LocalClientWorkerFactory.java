package pl.mrugames.nucleus.server.client_example;

import pl.mrugames.nucleus.server.client.*;

public class LocalClientWorkerFactory implements ClientWorkerFactory<String, String> {

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new LocalClientWorker(comm);
    }
}
