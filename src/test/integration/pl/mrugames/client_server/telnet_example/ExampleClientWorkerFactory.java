package pl.mrugames.client_server.telnet_example;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.ClientWorkerFactory;
import pl.mrugames.client_server.client.Comm;

public class ExampleClientWorkerFactory implements ClientWorkerFactory<String, String, String, String> {
    private final Runnable onShutdownCommand;

    public ExampleClientWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(Comm<String, String, String, String> comm, ClientInfo clientInfo) {
        return new ExampleClientWorker(comm, onShutdownCommand, clientInfo);
    }
}
