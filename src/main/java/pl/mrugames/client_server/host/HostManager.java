package pl.mrugames.client_server.host;

import pl.mrugames.client_server.client.ClientFactory;

import java.util.LinkedList;
import java.util.List;

public class HostManager {
    private final List<Host> hosts;

    public HostManager() {
        hosts = new LinkedList<>();
    }

    public synchronized void newHost(String name, int port, ClientFactory clientFactory) throws InterruptedException, FailedToStartException {
        Host host = new Host(name, port, clientFactory);
        host.start();
        boolean result = host.waitForSocketOpen();
        if (!result) {
            host.interrupt();
            throw new FailedToStartException();
        }

        hosts.add(host);
    }

    public synchronized void shutdown() throws InterruptedException {
        hosts.forEach(Host::interrupt);
        for (Host host : hosts) {
            host.join();
        }
    }
}
