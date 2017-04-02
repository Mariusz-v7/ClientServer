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

    private volatile boolean interrupted;
    private volatile Thread currentThread;

    ClientReaderThread(String name,
                       InputStream originalInputStream,
                       BlockingQueue<FrameType> received,
                       ClientReader<FrameType, StreamType> clientReader) {
        this.name = name;
        this.originalInputStream = originalInputStream;
        this.received = received;
        this.clientReader = clientReader;
    }

    void interrupt() {
        interrupted = true;
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

    void join() throws InterruptedException {
        if (currentThread != null) {
            currentThread.join();
        }
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        logger.info("[{}] Reader thread started!", name);

        try (StreamType inputStream = clientReader.prepare(originalInputStream)) {
            while (!interrupted && !currentThread.isInterrupted()) {
                received.add(clientReader.next(inputStream));
            }
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Reader thread has been stopped!", name);
        }
    }

    Thread getCurrentThread() {
        return currentThread;
    }
}
