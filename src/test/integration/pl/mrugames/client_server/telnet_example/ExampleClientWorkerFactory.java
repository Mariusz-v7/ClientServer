package pl.mrugames.client_server.telnet_example;

import pl.mrugames.client_server.client.*;

public class ExampleClientWorkerFactory implements ClientWorkerFactory<String, String, String, String> {
    private final Runnable shutdownServer;

    public ExampleClientWorkerFactory(Runnable shutdownServer) {
        this.shutdownServer = shutdownServer;
    }

    @Override
    public ClientWorker create(Comm<String, String, String, String> comm, ClientInfo clientInfo, KillMe killme) {
        return new ExampleClientWorker(shutdownServer, clientInfo, killme);
    }
}
