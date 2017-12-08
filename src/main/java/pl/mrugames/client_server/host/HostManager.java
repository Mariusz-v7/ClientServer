package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactoryV2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class HostManager implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    volatile Selector selector;
    volatile boolean started = false;
    final List<Host> hosts;

    public HostManager() throws IOException, InterruptedException {
        this.hosts = new CopyOnWriteArrayList<>();
    }

    public synchronized void newHost(String name, int port, ClientFactoryV2 clientFactory) {
        if (started) {
            throw new HostManagerIsRunningException("Host Manager is running. Please submit your hosts before starting Host Manager's thread!");
        }

        Host host = new Host(name, port, clientFactory);
        hosts.add(host);

        logger.info("New Host has been submitted: {}", host);
    }

    @Override
    public void run() {
        try {
            synchronized (this) {
                logger.info("Host Manager has been started in thread: {}", Thread.currentThread().getName());
                started = true;
            }

            selector = Selector.open();

            startHosts();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (selector.select() <= 0) {
                        continue;
                    }

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    selectionKeys.forEach(this::progressKey);
                    selectionKeys.clear();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to open selector!");
        } finally {
            shutdown();
        }
    }

    private void progressKey(SelectionKey selectionKey) {
        SelectableChannel channel = selectionKey.channel();

        if (selectionKey.isAcceptable()) {
            Host host = (Host) selectionKey.attachment();
            acceptConnection(host, (ServerSocketChannel) channel);
            return;
        }
    }

    void startHosts() {
        logger.info("Starting hosts");

        for (Host host : hosts) {
            logger.info("[{}] Host is starting", host.getName());

            try {
                ServerSocketChannel serverSocketChannel = serverSocketChannelFactory(host);
                host.setServerSocketChannel(serverSocketChannel);
            } catch (Exception e) {
                logger.error("[{}] Failed to start host", host.getName(), e);
                continue;
            }

            logger.info("[{}] Host started", host.getName());
        }

        logger.info("All hosts started");
    }

    void acceptConnection(Host host, ServerSocketChannel channel) {
        logger.info("[{}] New Client is connecting", host.getName());

        Socket socket = null;
        try {
            socket = accept(host, channel);
            host.getClientFactory().create(socket);
        } catch (Exception e) {
            logger.error("[{}] Error during client creation", host.getName(), e);

            if (socket != null) {
                logger.error("[{}] Closing client's socket", host.getName());

                try {
                    socket.close();
                    logger.error("[{}] Client's socket closed", host.getName());
                } catch (IOException e1) {
                    logger.error("[{}] Failed to close client's socket", host.getName(), e1);
                }
            }
        }

    }

    public synchronized void shutdown() {
        logger.info("Host Manager is stopping");

        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            logger.info("Closing connection: {}", key.attachment());

            try {
                closeChannel(key);
            } catch (Exception e) {
                logger.info("Failed to close connection: {}", key.attachment());
                continue;
            }

            logger.info("Connection closed: {}", key.attachment());
        }

        try {
            selector.close();
        } catch (IOException e) {
            logger.error("Failed to close selector", e);
        }

        logger.info("Host Manager has been stopped");
    }

    /**
     * Method created for mocking purposes.
     */
    ServerSocketChannel serverSocketChannelFactory(Host host) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(host.getPort()));

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, host);

        return serverSocketChannel;
    }

    /**
     * Method created for mocking purposes.
     */
    Socket accept(Host host, ServerSocketChannel channel) throws IOException {
        SocketChannel socketChannel = channel.accept();
        socketChannel.configureBlocking(true);

        logger.info("[{}] New client has been accepted: {}/{}", host.getName(), socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());

        return socketChannel.socket();
    }

    void closeChannel(SelectionKey key) throws IOException {
        key.channel().close();
    }

}
