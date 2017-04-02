package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.io.ClientReader;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

class ClientReaderThread<FrameType, StreamType extends AutoCloseable> implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientReaderThread.class);

    private final String name;
    private final BlockingQueue<FrameType> received;
    private final ClientReader<FrameType> clientReader;

    private volatile boolean interrupted;
    private volatile CountDownLatch shutdownSignal;

    ClientReaderThread(String name,
                       BlockingQueue<FrameType> received,
                       ClientReader<FrameType> clientReader) {
        this.name = name;
        this.received = received;
        this.clientReader = clientReader;
    }

    void interrupt() {
        interrupted = true;
    }

    void join() throws InterruptedException {
        if (shutdownSignal != null) {
            shutdownSignal.await();
        }
    }

    @Override
    public void run() {
        shutdownSignal = new CountDownLatch(1);
        logger.info("[{}] Reader thread started!", name);

        try {
            while (!interrupted && !Thread.currentThread().isInterrupted()) {
                received.add(clientReader.next());
            }
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Reader thread has been stopped!", name);
            shutdownSignal.countDown();
        }
    }

}
