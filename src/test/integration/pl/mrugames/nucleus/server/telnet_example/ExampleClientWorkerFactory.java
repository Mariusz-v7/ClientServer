package pl.mrugames.nucleus.server.telnet_example;

import pl.mrugames.nucleus.server.client.*;

public class ExampleClientWorkerFactory implements ClientWorkerFactory<String, String> {
    private final Runnable shutdownServer;

    public ExampleClientWorkerFactory(Runnable shutdownServer) {
        this.shutdownServer = shutdownServer;
    }

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new ExampleClientWorker(shutdownServer, clientInfo, controller);
    }
}
