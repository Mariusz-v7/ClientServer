package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class Client<In, Out, Reader extends Serializable, Writer extends Serializable> {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final List<Initializer> initializers;
    private final ClientWorker<In, Out> clientWorker;
    private final SocketChannel channel;
    private final Comm<In, Out, Reader, Writer> comm;
    private final ExecutorService requestExecutor;

    Client(String name,
           ExecutorService requestExecutor,
           List<Initializer> initializers,
           Comm<In, Out, Reader, Writer> comm,
           ClientWorker<In, Out> clientWorker,
           SocketChannel channel) {
        this.name = name;
        this.requestExecutor = requestExecutor;
        this.initializers = initializers;
        this.clientWorker = clientWorker;
        this.channel = channel;
        this.comm = comm;

        logger.info("[{}] New client has been created", name);
    }

    public Comm<In, Out, Reader, Writer> getComm() {
        return comm;
    }

    public ExecutorService getRequestExecutor() {
        return requestExecutor;
    }

    public String getName() {
        return name;
    }

    public List<Initializer> getInitializers() {
        return initializers;
    }

    public ClientWorker<In, Out> getClientWorker() {
        return clientWorker;
    }

    public void closeChannel() {
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("Failed to close channel", e);
        }
    }

    public ByteBuffer getReadBuffer() {
        return null;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}
