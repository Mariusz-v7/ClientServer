package pl.mrugames.commons.telnet_example;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.Comm;

public class ExampleClientWorkerFactory implements ClientWorkerFactory<String, String> {
    private final Runnable onShutdownCommand;

    public ExampleClientWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(String name, Comm<String, String> comm, Runnable shutdownSwitch) {
        return new ExampleClientWorker(name, comm, shutdownSwitch, onShutdownCommand);
    }
}
