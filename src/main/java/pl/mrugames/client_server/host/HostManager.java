package pl.mrugames.client_server.host;

import pl.mrugames.client_server.client.ClientFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HostManager {
    private final List<Host> hosts;

    public HostManager() {
        hosts = new CopyOnWriteArrayList<>();
    }

    public synchronized void newHost(String name, int port, ClientFactory clientFactory) throws InterruptedException {
        Host host = new Host(name, port, clientFactory);
        host.start();
        host.waitForSocketOpen();
        hosts.add(host);
    }

    public synchronized void shutdown() throws InterruptedException {
        hosts.forEach(Host::interrupt);
        for (Host host : hosts) {
            host.join();
        }
    }
}
