package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.codahale.metrics.MetricRegistry.name;

public class ClientFactory<In, Out, Reader extends Serializable, Writer extends Serializable> {
    private static final Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    private final AtomicLong clientId;
    private final String factoryName;
    private final String clientNamePrefix;
    private final ClientWorkerFactory<In, Out, Reader, Writer> clientWorkerFactory;
    private final ClientWatchdog watchdog;
    private final int bufferSize;
    private final List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories;

    private final Timer clientSendMetric;
    private final Timer clientReceiveMetric;

    ClientFactory(String factoryName,
                  String clientNamePrefix,
                  ClientWorkerFactory<In, Out, Reader, Writer> clientWorkerFactory,
                  List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories,
                  ClientWatchdog clientWatchdog,
                  int bufferSize
    ) {
        this.clientId = new AtomicLong();
        this.factoryName = factoryName;
        this.clientNamePrefix = clientNamePrefix;
        this.clientWorkerFactory = clientWorkerFactory;
        this.protocolFactories = protocolFactories;
        this.watchdog = clientWatchdog;
        this.bufferSize = bufferSize;

        this.clientSendMetric = Metrics.getRegistry().timer(name(ClientFactory.class, "client", "send"));
        this.clientReceiveMetric = Metrics.getRegistry().timer(name(ClientFactory.class, "client", "receive"));
    }

    public Client<In, Out, Reader, Writer> create(SocketChannel channel, TaskExecutor taskExecutor) throws Exception {
        if (!watchdog.isRunning()) {
            throw new IllegalStateException("Client Watchdog is dead! Cannot accept new connection.");
        }

        try {
            logger.info("[{}] New client is being created!", factoryName);

            String clientName = clientNamePrefix + "-" + clientId.incrementAndGet();
            ClientInfo clientInfo = new ClientInfo(clientName, channel.socket());

            Socket socket = channel.socket();

            ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);
            readBuffer.flip();

            ByteBuffer writeBuffer = ByteBuffer.allocate(bufferSize);

            Comm comm = createComms(clientName, channel, readBuffer, writeBuffer);

            KillMe killMe = new KillMe();
            ClientWorker<In, Out> clientWorker = createWorker(clientName, comm, clientInfo, killMe);

            Client<In, Out, Reader, Writer> client = createClient(clientName, taskExecutor, comm, clientWorker, channel, readBuffer);
            killMe.setClient(client);

            watchdog.register(client);

            logger.info("[{}] New client has been created: {}!", factoryName, client.getName());
            return client;
        } catch (Exception e) {
            closeChannel(channel);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    Comm createComms(String clientName, SocketChannel channel, ByteBuffer readBuffer, ByteBuffer writeBuffer) throws IOException {
        logger.info("[{}] Creating comms for client: {}", factoryName, clientName);

        String defaultProtocol = null;
        Map<String, Protocol<? extends Serializable, ? extends Serializable>> protocols = new HashMap<>();

        for (ProtocolFactory factory : protocolFactories) {
            Protocol protocol = factory.create(writeBuffer, readBuffer);
            if (defaultProtocol == null) {
                defaultProtocol = protocol.getName();
            }

            protocols.put(protocol.getName(), protocol);
        }

        Comm comm = new Comm(
                protocols,
                writeBuffer,
                channel,
                clientSendMetric,
                clientReceiveMetric,
                defaultProtocol);

        logger.info("[{}] Comms has been created for client: {}", factoryName, clientName);

        return comm;
    }

    ClientWorker<In, Out> createWorker(String clientName, Comm comm, ClientInfo clientInfo, KillMe killMe) {
        logger.info("[{}] Creating client worker for client: {}", factoryName, clientName);

        ClientWorker<In, Out> clientWorker = clientWorkerFactory.create(comm, clientInfo, killMe);

        logger.info("[{}] Client worker has been created for client: {}", factoryName, clientName);

        return clientWorker;
    }

    Client<In, Out, Reader, Writer> createClient(String clientName,
                                                 TaskExecutor taskExecutor,
                                                 Comm comm,
                                                 ClientWorker<In, Out> clientWorker,
                                                 SocketChannel channel,
                                                 ByteBuffer readBuffer
    ) {
        return new Client<>(clientName, taskExecutor, comm, clientWorker, channel, readBuffer);
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

}
