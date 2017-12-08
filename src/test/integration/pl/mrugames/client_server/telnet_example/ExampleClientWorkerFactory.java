package pl.mrugames.client_server.telnet_example;

import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorkerFactoryV2;
import pl.mrugames.client_server.client.CommV2;

public class ExampleClientWorkerFactory implements ClientWorkerFactoryV2<String, String, String, String> {
    private final Runnable onShutdownCommand;

    public ExampleClientWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public Runnable create(CommV2<String, String, String, String> comm, ClientInfo clientInfo) {
        return new ExampleClientWorker(comm, onShutdownCommand, clientInfo);
    }
}
