package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class Comm {
    private final static Logger logger = LoggerFactory.getLogger(Comm.class);

    private volatile ClientWriter<? extends Serializable> clientWriter;
    private volatile ClientReader<? extends Serializable> clientReader;
    private volatile FilterProcessor inputFilterProcessor;
    private volatile FilterProcessor outputFilterProcessor;

    private final Map<String, Protocol<? extends Serializable, ? extends Serializable>> protocols;

    private final Timer sendMetric;
    private final Timer receiveMetric;
    private final ByteBuffer writeBuffer;
    private final SocketChannel socketChannel;
    private final Lock readBufferLock;
    private final Lock writeBufferLock;

    private volatile Instant lastDataSent;
    private volatile Instant lastDataReceived;

    Comm(Map<String, Protocol<? extends Serializable, ? extends Serializable>> protocols,
         ByteBuffer writeBuffer,
         Lock readBufferLock,
         Lock writeBufferLock,
         SocketChannel socketChannel,
         Timer sendMetric,
         Timer receiveMetric,
         String defaultProtocol) {
        this.protocols = protocols;
        this.writeBuffer = writeBuffer;
        this.socketChannel = socketChannel;
        this.readBufferLock = readBufferLock;
        this.writeBufferLock = writeBufferLock;

        this.sendMetric = sendMetric;
        this.receiveMetric = receiveMetric;

        Instant now = Instant.now();

        this.lastDataReceived = now;
        this.lastDataSent = now;

        switchProtocol(defaultProtocol);
    }

    /**
     * The client may request to change protocol.
     * Before doing so, client <b>should wait until all his requests are finished</b>.
     * Client should resume sending requests only when he is absolutely sure that switching procedure was completed.
     */
    public synchronized void switchProtocol(String protocol) { // todo: lock both buffers
        Protocol<? extends Serializable, ? extends Serializable> toSwitch = protocols.get(protocol);

        if (toSwitch == null) {
            throw new IllegalArgumentException("No defined protocol: '" + protocol + "'");
        }

        this.clientReader = toSwitch.getClientReader();
        this.clientWriter = toSwitch.getClientWriter();
        this.inputFilterProcessor = toSwitch.getInputFilterProcessor();
        this.outputFilterProcessor = toSwitch.getOutputFilterProcessor();
    }

    @SuppressWarnings("unchecked")
    public synchronized void send(Object frame) throws Exception {
        try (Timer.Context ignored = sendMetric.time()) {
            logger.debug("[SEND] Transforming to raw frame: '{}'", frame);

            Optional<? extends Serializable> result = outputFilterProcessor.filter(frame);
            if (result.isPresent()) {
                Serializable rawFrame = result.get();
                logger.debug("[SEND] Frame after transformation: '{}'", rawFrame);
                ((ClientWriter<Serializable>) clientWriter).write(rawFrame);

                writeBuffer.flip();
                try {
                    socketChannel.write(writeBuffer);
                } finally {
                    writeBuffer.compact();
                }

            } else {
                logger.debug("[SEND] Frame '{}' filtered out!", frame);
            }

            lastDataSent = Instant.now();
        }
    }

    public boolean canRead() throws Exception {
        readBufferLock.lock();
        try {
            return clientReader.isReady();
        } finally {
            readBufferLock.unlock();
        }
    }

    @Nullable
    public synchronized Object receive() throws Exception {
        if (!clientReader.isReady()) {
            logger.debug("[RECEIVE] Reader is not ready!");
            return null;
        }

        try (Timer.Context ignored = receiveMetric.time()) {

            Object frame;

            Serializable rawFrame = clientReader.read();

            logger.debug("[RECEIVE] Transforming from raw frame: '{}'", rawFrame);

            Optional<? extends Serializable> result = inputFilterProcessor.filter(rawFrame);
            if (result.isPresent()) {
                frame = result.get();
                logger.debug("[RECEIVE] Frame after transformation: '{}'", frame);

                lastDataReceived = Instant.now();
                return frame;
            } else {
                logger.debug("[RECEIVE] Frame '{}' filtered out!", rawFrame);
            }

            return null;
        }
    }

    Instant getLastDataSent() {
        return lastDataSent;
    }

    Instant getLastDataReceived() {
        return lastDataReceived;
    }

    ClientWriter<? extends Serializable> getClientWriter() {
        return clientWriter;
    }

    ClientReader<? extends Serializable> getClientReader() {
        return clientReader;
    }

    FilterProcessor getInputFilterProcessor() {
        return inputFilterProcessor;
    }

    FilterProcessor getOutputFilterProcessor() {
        return outputFilterProcessor;
    }

    ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    Map<String, Protocol<? extends Serializable, ? extends Serializable>> getProtocols() {
        return protocols;
    }

    Lock getReadBufferLock() {
        return readBufferLock;
    }

    Lock getWriteBufferLock() {
        return writeBufferLock;
    }
}
