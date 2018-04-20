package pl.mrugames.nucleus.server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.server.tasks.TaskExecutor;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

public class Client<In, Out> {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final ClientWorker<In, Out> clientWorker;
    private final SocketChannel channel;
    private final Comm comm;
    private final TaskExecutor taskExecutor;
    private final ByteBuffer readBuffer;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final Lock readBufferLock;
    private final Lock writeBufferLock;
    private final long connectionTimeoutSeconds;
    private final long requestTimeoutSeconds;
    private final Instant created;

    private volatile ProtocolSwitch protocolSwitch;

    Client(String name,
           TaskExecutor taskExecutor,
           Comm comm,
           ClientWorker<In, Out> clientWorker,
           SocketChannel channel,
           ByteBuffer readBuffer,
           Lock readBufferLock,
           Lock writeBufferLock,
           long connectionTimeoutSeconds,
           long requestTimeoutSeconds) {
        this.name = name;
        this.taskExecutor = taskExecutor;
        this.clientWorker = clientWorker;
        this.channel = channel;
        this.comm = comm;
        this.readBuffer = readBuffer;
        this.readBufferLock = readBufferLock;
        this.writeBufferLock = writeBufferLock;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.created = Instant.now();

        logger.info("[{}] New client has been created", name);
    }

    public Comm getComm() {
        return comm;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public String getName() {
        return name;
    }

    public ClientWorker<In, Out> getClientWorker() {
        return clientWorker;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public AtomicBoolean getShutdown() {
        return shutdown;
    }

    public void scheduleProtocolSwitch(ProtocolSwitch protocolSwitch) {
        this.protocolSwitch = protocolSwitch;
    }

    public ProtocolSwitch getProtocolSwitch() {
        return protocolSwitch;
    }

    public Lock getReadBufferLock() {
        return readBufferLock;
    }

    public Lock getWriteBufferLock() {
        return writeBufferLock;
    }

    public long getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public long getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public Instant getCreated() {
        return created;
    }
}
