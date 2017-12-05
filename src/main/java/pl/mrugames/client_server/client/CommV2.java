package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.filters.FilterProcessorV2;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

public class CommV2<In, Out, Writer extends Serializable, Reader extends Serializable> {
    private final static Logger logger = LoggerFactory.getLogger(CommV2.class);

    private final ClientWriter<Writer> clientWriter;
    private final ClientReader<Reader> clientReader;
    private final FilterProcessorV2 inputFilterProcessor;
    private final FilterProcessorV2 outputFilterProcessor;

    private volatile Instant lastDataSent;
    private volatile Instant lastDataReceived;

    CommV2(ClientWriter<Writer> clientWriter, ClientReader<Reader> clientReader, FilterProcessorV2 inputFilterProcessor, FilterProcessorV2 outputFilterProcessor) {
        this.clientWriter = clientWriter;
        this.clientReader = clientReader;
        this.inputFilterProcessor = inputFilterProcessor;
        this.outputFilterProcessor = outputFilterProcessor;

        Instant now = Instant.now();

        this.lastDataReceived = now;
        this.lastDataSent = now;
    }

    public synchronized void send(Out frame) throws Exception {
        logger.debug("[SEND] Transforming to raw frame: '{}'", frame);

        Optional<Writer> result = outputFilterProcessor.filter(frame);
        if (result.isPresent()) {
            Writer rawFrame = result.get();
            logger.debug("[SEND] Frame after transformation: '{}'", rawFrame);
            clientWriter.next(rawFrame);
        } else {
            logger.debug("[SEND] Frame '{}' filtered out!", frame);
        }

        lastDataSent = Instant.now();
    }

    public synchronized In receive() throws Exception {
        In frame;

        do {
            Reader rawFrame = clientReader.next();

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
        } while (!Thread.currentThread().isInterrupted());

        throw new InterruptedException("Thread interrupted before receiving message!");
    }

    Instant getLastDataSent() {
        return lastDataSent;
    }

    Instant getLastDataReceived() {
        return lastDataReceived;
    }
}
