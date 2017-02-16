package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.io.ClientReader;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

class ClientReaderThread<FrameType, StreamType extends AutoCloseable> implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientReaderThread.class);

    private final String name;
    private final InputStream originalInputStream;
    private final BlockingQueue<FrameType> received;
    private final ClientReader<FrameType, StreamType> clientReader;

    ClientReaderThread(String name,
                       InputStream originalInputStream,
                       BlockingQueue<FrameType> received,
                       ClientReader<FrameType, StreamType> clientReader) {
        this.name = name;
        this.originalInputStream = originalInputStream;
        this.received = received;
        this.clientReader = clientReader;
    }

    @Override
    public void run() {
        logger.info("[{}] Reader thread started!", name);

        try (StreamType inputStream = clientReader.prepare(originalInputStream)) {
            while (!Thread.currentThread().isInterrupted()) {
                received.add(clientReader.next(inputStream));
            }
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }

        logger.info("[{}] Reader thread stopped!", name);
    }

}
