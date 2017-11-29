package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.*;
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

        Host host = new Host(name, port, clientFactory, ServerSocketChannel.open(), selector);
        hosts.add(host);

        logger.info("New Host has been created: {}", host);
    }

    @Override
    public void run() {
        logger.info("Host Manager has been started in thread: {}", Thread.currentThread().getName());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (selector.select() <= 0) {
                    logger.warn("Selected 0 keys...");
                    continue;
                }

                selector.selectedKeys().forEach(this::progressKey);
                cleanUp();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info("Host Manager is stopping");

        try {
            shutdown();
        } catch (IOException e) {
            logger.info("Failed to stop host manager!", e);
            return;
        }

        logger.info("Host Manager has been stopped successfully");
    }

    private void progressKey(SelectionKey selectionKey) {
        SelectableChannel channel = selectionKey.channel();

        if (selectionKey.isAcceptable()) {
            Host host = (Host) selectionKey.attachment();
            acceptConnection(host, (ServerSocketChannel) channel);
            return;
        }
    }

    void acceptConnection(Host host, ServerSocketChannel channel) {
        logger.info("[{}] New Client is connecting", host.getName());

        try {
            SocketChannel socketChannel = channel.accept();
            socketChannel.configureBlocking(true); //TODO

            Socket socket = socketChannel.socket();

            logger.info("[{}] New client has been accepted: {}/{}", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());

            host.getClientFactory().create(socket); // todo
        } catch (IOException e) {
            logger.error("[{}] Error during socket accept", host.getName(), e);
        }
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
