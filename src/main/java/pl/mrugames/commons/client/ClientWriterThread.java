package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.io.ClientWriter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ClientWriterThread<FrameType, StreamType extends AutoCloseable> implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientWriterThread.class);

    private final String name;
    private final BlockingQueue<FrameType> toSend;
    private final ClientWriter<FrameType> clientWriter;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private volatile boolean interrupted;
    private volatile CountDownLatch shutdownSignal;

    ClientWriterThread(String name,
                       BlockingQueue<FrameType> toSend,
                       ClientWriter<FrameType> clientWriter,
                       long timeout,
                       TimeUnit timeoutUnit) {
        this.name = name;
        this.toSend = toSend;
        this.clientWriter = clientWriter;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
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
        logger.info("[{}] Writer thread started!", name);

        try {
            while (!interrupted && !Thread.currentThread().isInterrupted()) {
                FrameType frame = toSend.poll(timeout, timeoutUnit);
                if (frame != null) {
                    clientWriter.next(frame);
                } else {
                    throw new TimeoutException("No frames to send since " + timeout + " " + timeoutUnit);
                }
            }
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Writer thread has been stopped!", name);
            shutdownSignal.countDown();;
        }
    }
}
