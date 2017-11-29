package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;
import java.util.List;

public class HostManager implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    final Selector selector;
    final List<Host> hosts;

    public HostManager() throws IOException {
        this.selector = Selector.open();
        this.hosts = new LinkedList<>();
    }

    public synchronized void newHost(String name, int port, ClientFactory clientFactory) throws IOException {
        if (!selector.isOpen()) {
            throw new HostManagerIshShutDownException();
        }

        hosts.add(new Host(name, port, clientFactory, ServerSocketChannel.open(), selector));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

        }
    }

    public synchronized void shutdown() throws IOException {
        selector.close();
        hosts.forEach(Host::shutdown);
        hosts.clear();
    }
}
