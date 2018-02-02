package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientWatchdog;
import pl.mrugames.client_server.tasks.ClientRequestTask;
import pl.mrugames.client_server.tasks.ClientShutdownTask;
import pl.mrugames.client_server.tasks.NewClientAcceptTask;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class HostManager implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExecutorService executorService;
    private final CountDownLatch shutdownSignal = new CountDownLatch(1);
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final boolean manageExecutorService;
    private final TaskExecutor taskExecutor;
    private final ExecutorService maintenanceExecutor;
    private final ClientWatchdog clientWatchdog;
    final List<Host> hosts;

    volatile Selector selector;
    volatile boolean started = false;
    private volatile boolean stopped = false;

    public static HostManager create(int numThreads) {
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        return new HostManager(executorService, true);
    }

    /**
     * When you use this method, you have to manage executorService on your own (eg. start it and stop it)
     */
    public static HostManager create(ExecutorService executorService) {
        return new HostManager(executorService, false);
    }

    HostManager(ExecutorService clientExecutor, boolean manageExecutorService) {
        this(clientExecutor, manageExecutorService, new TaskExecutor(clientExecutor), Executors.newSingleThreadExecutor(), new ClientWatchdog());
    }

    HostManager(ExecutorService clientExecutor, boolean manageExecutorService, TaskExecutor taskExecutor,
                ExecutorService maintenanceExecutor, ClientWatchdog clientWatchdog) {
        this.hosts = new CopyOnWriteArrayList<>();
        this.executorService = clientExecutor;
        this.manageExecutorService = manageExecutorService;
        this.taskExecutor = taskExecutor;
        this.maintenanceExecutor = maintenanceExecutor;
        this.clientWatchdog = clientWatchdog;
    }

    public synchronized void newHost(String name, int port, ClientFactory clientFactory) {
        if (started) {
            throw new HostManagerIsRunningException("Host Manager is running. Please submit your hosts before starting Host Manager's thread!");
        }

        Host host = new Host(name, port, clientFactory);
        hosts.add(host);

        logger.info("New Host has been submitted: {}", host);
    }

    @Override
    public void run() {
        startSignal.countDown();

        try {
            maintenanceExecutor.execute(clientWatchdog);

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
                    synchronized (this) {
                        selectionKeys.forEach(this::progressKey);
                        selectionKeys.clear();
                    }
                } catch (ClosedSelectorException e) {
                    logger.debug(e.getMessage(), e);
                    break;
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to open selector!");
        } finally {
            try {
                shutdown();
            } finally {
                maintenanceExecutor.shutdownNow();

                try {
                    if (!maintenanceExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        logger.error("Maintenance Executor did not terminate.");
                    }
                } catch (InterruptedException e) {
                    logger.error("Failed to wait for executor termination", e);
                }

                if (manageExecutorService) {
                    executorService.shutdownNow();

                    try {
                        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                            logger.error("Client Executor did not terminate.");
                        }

                    } catch (InterruptedException e) {
                        logger.error("Failed to wait for executor termination", e);
                    }
                }

                shutdownSignal.countDown();
            }
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
        } catch (CancelledKeyException e) {
            logger.debug("Failed to progress key: {}", selectionKey, e);
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

        client.getReadBufferLock().lock();
        try {
            readToBuffer(client.getReadBuffer(), client.getChannel());

            ClientRequestTask clientRequestTask = new ClientRequestTask(client);

            client.getTaskExecutor().submit(clientRequestTask);
        } catch (IOException e) {
            logger.debug("[{}] Failed to read from client", client.getName(), e);
        } catch (Exception e) {
            logger.error("[{}] Failed to read from client", client.getName(), e);
            client.getTaskExecutor().submit(new ClientShutdownTask(client));
        } finally {
            client.getReadBufferLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    void accept(ServerSocketChannel channel, Host host) {
        SocketChannel socketChannel = null;
        try {
            socketChannel = channel.accept();

            configure(socketChannel);

            NewClientAcceptTask acceptTask = new NewClientAcceptTask(host.getName(), host.getClientFactory(), socketChannel, taskExecutor, clientWatchdog);
            Future<Client> result = taskExecutor.submit(acceptTask);

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
        if (stopped) {
            return;
        }

        logger.info("Host Manager is stopping");

        try {
            Set<SelectionKey> keys = selector.keys();
            List<SelectableChannel> channelsToClose = new LinkedList<>();
            for (SelectionKey key : keys) {
                channelsToClose.add(key.channel());
            }

            for (SelectableChannel channel : channelsToClose) {
                logger.info("Closing connection: {}", channel);

                try {
                    closeChannel(channel);
                } catch (Exception e) {
                    logger.info("Failed to close connection: {}", channel);
                    continue;
                }

                logger.info("Connection closed: {}", channel);
            }
        } catch (ClosedSelectorException e) {
            logger.warn("Selector is closed already", e);
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        } finally {
            try {
                selector.close();
            } catch (Exception e) {
                logger.error("Failed to close selector", e);
            }
        }

        logger.info("Host Manager has been stopped");
        stopped = true;
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

    void closeChannel(SelectableChannel channel) throws IOException {
        channel.close();
    }

    /**
     * This method is executed in host manager's thread to prevent from spawning multiple tasks (selector returns this key as long as the data is available)
     */
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

    public boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return shutdownSignal.await(timeout, timeUnit);
    }

    public boolean awaitStart(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return startSignal.await(timeout, timeUnit);
    }
}
