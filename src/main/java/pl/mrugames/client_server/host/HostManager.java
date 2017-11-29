package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

        Host host = new Host(name, port, clientFactory, ServerSocketChannel.open(), selector);
        hosts.add(host);

        logger.info("New Host has been created: {}", host);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (selector.select() <= 0) {
                    logger.warn("Selected 0 keys...");
                    continue;
                }

                progressKeys(selector.selectedKeys());
                cleanUp();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    void progressKeys(Set<SelectionKey> selectionKeys) {

    }

    void cleanUp() {
        //TODO: find closed hosts and remove them
    }

    public synchronized void shutdown() throws IOException {
        selector.close();
        hosts.forEach(Host::shutdown);
        hosts.clear();
    }
}
