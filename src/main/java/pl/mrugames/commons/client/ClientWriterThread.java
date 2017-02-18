package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.io.ClientWriter;

import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ClientWriterThread<FrameType, StreamType extends AutoCloseable> implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientWriterThread.class);

    private final String name;
    private final OutputStream originalOutputStream;
    private final BlockingQueue<FrameType> toSend;
    private final ClientWriter<FrameType, StreamType> clientWriter;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    ClientWriterThread(String name,
                       OutputStream originalOutputStream,
                       BlockingQueue<FrameType> toSend,
                       ClientWriter<FrameType, StreamType> clientWriter,
                       long timeout,
                       TimeUnit timeoutUnit) {
        this.name = name;
        this.originalOutputStream = originalOutputStream;
        this.toSend = toSend;
        this.clientWriter = clientWriter;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public void run() {
        logger.info("[{}] Writer thread started!", name);

        try (StreamType inputStream = clientWriter.prepare(originalOutputStream)) {
            while (!Thread.currentThread().isInterrupted()) {
                FrameType frame = toSend.poll(timeout, timeoutUnit);
                if (frame != null) {
                    clientWriter.next(inputStream, frame);
                } else {
                    throw new TimeoutException("No frames to send since " + timeout + " " + timeoutUnit);
                }
            }
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Writer thread has been stopped!", name);
        }
    }
}
