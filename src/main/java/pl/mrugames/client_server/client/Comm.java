package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

public class Comm<In, Out, Reader extends Serializable, Writer extends Serializable> {
    private final static Logger logger = LoggerFactory.getLogger(Comm.class);

    private final ClientWriter<Writer> clientWriter;
    private final ClientReader<Reader> clientReader;
    private final FilterProcessor inputFilterProcessor;
    private final FilterProcessor outputFilterProcessor;
    private final Timer sendMetric;
    private final Timer receiveMetric;

    private volatile Instant lastDataSent;
    private volatile Instant lastDataReceived;

    Comm(ClientWriter<Writer> clientWriter,
         ClientReader<Reader> clientReader,
         FilterProcessor inputFilterProcessor,
         FilterProcessor outputFilterProcessor,
         Timer sendMetric,
         Timer receiveMetric) {
        this.clientWriter = clientWriter;
        this.clientReader = clientReader;
        this.inputFilterProcessor = inputFilterProcessor;
        this.outputFilterProcessor = outputFilterProcessor;
        this.sendMetric = sendMetric;
        this.receiveMetric = receiveMetric;

        Instant now = Instant.now();

        this.lastDataReceived = now;
        this.lastDataSent = now;
    }

    public synchronized boolean canRead() throws Exception {
        return clientReader.isReady();
    }

    public synchronized void send(Out frame) throws Exception {
        try (Timer.Context ignored = sendMetric.time()) {
            logger.debug("[SEND] Transforming to raw frame: '{}'", frame);

            Optional<Writer> result = outputFilterProcessor.filter(frame);
            if (result.isPresent()) {
                Writer rawFrame = result.get();
                logger.debug("[SEND] Frame after transformation: '{}'", rawFrame);
                clientWriter.write(rawFrame);
            } else {
                logger.debug("[SEND] Frame '{}' filtered out!", frame);
            }

            lastDataSent = Instant.now();
        }
    }

    @Nullable
    public synchronized In receive() throws Exception {
        try (Timer.Context ignored = receiveMetric.time()) {
            In frame;

            Reader rawFrame = clientReader.read();

            logger.debug("[RECEIVE] Transforming from raw frame: '{}'", rawFrame);

            Optional<In> result = inputFilterProcessor.filter(rawFrame);
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

    ClientWriter<Writer> getClientWriter() {
        return clientWriter;
    }

    ClientReader<Reader> getClientReader() {
        return clientReader;
    }

    FilterProcessor getInputFilterProcessor() {
        return inputFilterProcessor;
    }

    FilterProcessor getOutputFilterProcessor() {
        return outputFilterProcessor;
    }
}
