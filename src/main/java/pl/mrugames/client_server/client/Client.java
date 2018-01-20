package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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

    private volatile ProtocolSwitch protocolSwitch;

    Client(String name,
           TaskExecutor taskExecutor,
           Comm comm,
           ClientWorker<In, Out> clientWorker,
           SocketChannel channel,
           ByteBuffer readBuffer,
           Lock readBufferLock,
           Lock writeBufferLock) {
        this.name = name;
        this.taskExecutor = taskExecutor;
        this.clientWorker = clientWorker;
        this.channel = channel;
        this.comm = comm;
        this.readBuffer = readBuffer;
        this.readBufferLock = readBufferLock;
        this.writeBufferLock = writeBufferLock;

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
}
