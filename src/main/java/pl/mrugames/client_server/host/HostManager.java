package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.tasks.ClientRequestTask;
import pl.mrugames.client_server.tasks.ClientShutdownTask;
import pl.mrugames.client_server.tasks.NewClientAcceptTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class HostManager implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    volatile Selector selector;
    volatile boolean started = false;
    final List<Host> hosts;

    public HostManager() throws IOException, InterruptedException {
        this.hosts = new CopyOnWriteArrayList<>();
    }

    public synchronized void newHost(String name, int port, ClientFactory clientFactory, ExecutorService clientExecutor) {
        if (started) {
            throw new HostManagerIsRunningException("Host Manager is running. Please submit your hosts before starting Host Manager's thread!");
        }

        Host host = new Host(name, port, clientFactory, clientExecutor);
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

    @SuppressWarnings("unchecked")
    private void progressKey(SelectionKey selectionKey) {
        try {
            SelectableChannel channel = selectionKey.channel();

            if (selectionKey.isAcceptable()) {
                Host host = (Host) selectionKey.attachment();
                accept((ServerSocketChannel) channel, host);
            } else if (selectionKey.isReadable()) {
                Future<Client> client = (Future<Client>) selectionKey.attachment();
                read(client, (SocketChannel) channel);
            }
        } catch (Exception e) {
            logger.error("Failed to progress key: {}", selectionKey, e);
        }
    }

    @SuppressWarnings("unchecked")
    void read(Future<Client> clientFuture, SocketChannel socketChannel) {
        Client client;

        try {
            if (!clientFuture.isDone()) {
                logger.debug("Client creation is in progress still...");
                return;
            }

            client = clientFuture.get();
        } catch (Exception e) {
            logger.error("Client's future ended with exception!", e);

            try {
                closeClientChannel(socketChannel);
            } catch (IOException e1) {
                logger.error("Failed to close client's channel", e1);
            }

            return;
        }

        try {
            readToBuffer(client.getReadBuffer(), client.getChannel());

            ClientRequestTask clientRequestTask = new ClientRequestTask(client);

            client.getTaskExecutor().submit(clientRequestTask);
        } catch (IOException e) {
            logger.debug("[{}] Failed to read from client", client.getName(), e);
        } catch (Exception e) {
            logger.error("[{}] Failed to read from client", client.getName(), e);
            client.getTaskExecutor().submit(new ClientShutdownTask(client));
        }
    }

    @SuppressWarnings("unchecked")
    void accept(ServerSocketChannel channel, Host host) {
        SocketChannel socketChannel = null;
        try {
            socketChannel = channel.accept();

            configure(socketChannel);

            NewClientAcceptTask acceptTask = new NewClientAcceptTask(host.getName(), host.getClientFactory(), socketChannel, host.getTaskExecutor());
            Future<Client> result = host.getTaskExecutor().submit(acceptTask);

            register(socketChannel, result);
        } catch (Exception e) {
            if (socketChannel != null) {
                try {
                    closeClientChannel(socketChannel);
                } catch (IOException e1) {
                    logger.error("[{}] Failed to close channel", host.getName(), e1);
                }
            }
            logger.error("[{}] Failed to accept connection", host.getName(), e);
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

    void closeChannel(SelectionKey key) throws IOException {
        key.channel().close();
    }

    void readToBuffer(ByteBuffer readBuffer, SocketChannel socketChannel) throws IOException {
        readBuffer.compact();
        try {
            socketChannel.read(readBuffer);
        } finally {
            readBuffer.flip();
        }
    }

    /**
     * Mocking purposes
     */
    void configure(SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);
    }

    /**
     * Mocking purposes
     */
    void register(SocketChannel socketChannel, Future<Client> clientFuture) throws ClosedChannelException {
        socketChannel.register(selector, SelectionKey.OP_READ, clientFuture);
    }

    /**
     * Mocking purposes
     */
    void closeClientChannel(SocketChannel channel) throws IOException {
        channel.close();
    }
}
