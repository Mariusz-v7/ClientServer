package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientFactory<In, Out> {
    private static final Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    private final AtomicLong clientId;
    private final String factoryName;
    private final String clientNamePrefix;
    private final ClientWorkerFactory<In, Out> clientWorkerFactory;
    private final int bufferSize;
    private final List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories;
    private final long connectionTimeoutSeconds;
    private final long requestTimeoutSeconds;

    ClientFactory(String factoryName,
                  String clientNamePrefix,
                  ClientWorkerFactory<In, Out> clientWorkerFactory,
                  List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories,
                  int bufferSize,
                  long connectionTimeoutSeconds,
                  long requestTimeoutSeconds
    ) {
        this.clientId = new AtomicLong();
        this.factoryName = factoryName;
        this.clientNamePrefix = clientNamePrefix;
        this.clientWorkerFactory = clientWorkerFactory;
        this.protocolFactories = protocolFactories;
        this.bufferSize = bufferSize;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public Client<In, Out> create(SocketChannel channel, TaskExecutor taskExecutor, ConnectionWatchdog watchdog) throws Exception {
        try {
            logger.info("[{}] New client is being created!", factoryName);

            String clientName = clientNamePrefix + "-" + clientId.incrementAndGet();
            ClientInfo clientInfo = new ClientInfo(clientName, channel.socket());

            ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);
            readBuffer.flip();

            ByteBuffer writeBuffer = ByteBuffer.allocate(bufferSize);

            Lock readLock = new ReentrantLock();
            Lock writeLock = new ReentrantLock();
            Comm comm = createComms(clientName, channel, readBuffer, writeBuffer, readLock, writeLock);

            ClientController clientController = new ClientController();
            ClientWorker<In, Out> clientWorker = createWorker(clientName, comm, clientInfo, clientController);

            Client<In, Out> client = createClient(clientName, taskExecutor, comm, clientWorker, channel, readBuffer, readLock, writeLock);
            clientController.setClient(client);

            watchdog.register(client);

            logger.info("[{}] New client has been created: {}!", factoryName, client.getName());
            return client;
        } catch (Exception e) {
            closeChannel(channel);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    Comm createComms(String clientName, SocketChannel channel, ByteBuffer readBuffer, ByteBuffer writeBuffer, Lock readBufferLock, Lock writeBufferLock) throws IOException {
        logger.info("[{}] Creating comms for client: {}", factoryName, clientName);

        String defaultProtocol = null;
        Map<String, Protocol<? extends Serializable, ? extends Serializable>> protocols = new HashMap<>();

        for (ProtocolFactory factory : protocolFactories) {
            Protocol protocol = factory.create(writeBuffer, readBuffer);
            if (defaultProtocol == null) {
                defaultProtocol = protocol.getName();
            }

            if (protocols.containsKey(protocol.getName())) {
                throw new IllegalArgumentException("Duplicate protocol name: '" + protocol.getName() + "'");
            }

            protocols.put(protocol.getName(), protocol);
        }

        Comm comm = new Comm(
                protocols,
                writeBuffer,
                readBufferLock,
                writeBufferLock,
                channel,
                defaultProtocol);

        logger.info("[{}] Comms has been created for client: {}", factoryName, clientName);

        return comm;
    }

    ClientWorker<In, Out> createWorker(String clientName, Comm comm, ClientInfo clientInfo, ClientController clientController) {
        logger.info("[{}] Creating client worker for client: {}", factoryName, clientName);

        ClientWorker<In, Out> clientWorker = clientWorkerFactory.create(comm, clientInfo, clientController);

        logger.info("[{}] Client worker has been created for client: {}", factoryName, clientName);

        return clientWorker;
    }

    Client<In, Out> createClient(String clientName,
                                 TaskExecutor taskExecutor,
                                 Comm comm,
                                 ClientWorker<In, Out> clientWorker,
                                 SocketChannel channel,
                                 ByteBuffer readBuffer,
                                 Lock readLock,
                                 Lock writeLock
    ) {
        return new Client<>(clientName, taskExecutor, comm, clientWorker, channel, readBuffer, readLock, writeLock, connectionTimeoutSeconds, requestTimeoutSeconds);
    }

    void closeChannel(SocketChannel channel) {
        try {
            logger.error("[{}] Exception during client initialization", factoryName);
            logger.error("[{}] Closing the socket", factoryName);
            channel.close();
            logger.error("[{}] Socket closed", factoryName);
        } catch (IOException e1) {
            logger.error("[{}] Failed to close the socket", factoryName, e1);
        }
    }

    public long getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }
}
