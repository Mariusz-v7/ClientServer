package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.io.ClientWriter;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ClientWriterThread<Input, Output extends Serializable> implements Runnable {
    private final static class Terminator implements Serializable {
    }

    private final static Terminator TERMINATOR = new Terminator();

    private final static Logger logger = LoggerFactory.getLogger(ClientWriterThread.class);

    private final String name;
    private final BlockingQueue<Input> toSend;
    private final ClientWriter<Output> clientWriter;
    private final long timeout;
    private final TimeUnit timeoutUnit;


    private volatile boolean interrupted;
    private volatile CountDownLatch shutdownSignal;

    ClientWriterThread(String name,
                       BlockingQueue<Input> toSend,
                       ClientWriter<Output> clientWriter,
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

        @SuppressWarnings("unchecked")
        Input terminator = (Input) TERMINATOR;
        toSend.add(terminator);  // instead of interrupting the thread, add terminator to the queue
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
                Input frame = toSend.poll(timeout, timeoutUnit);
                if (frame == TERMINATOR) {
                    logger.info("[{}] Terminator received", name);
                    break;
                }

                if (frame != null) {
                    Optional<Output> transformed = filter(frame);
                    if (transformed.isPresent()) {
                        clientWriter.next(transformed.get());
                    }
                } else {
                    throw new TimeoutException("No frames to send since " + timeout + " " + timeoutUnit);
                }
            }
        } catch (InterruptedException e) {
            logger.warn("[{}] Thread has been interrupted", name);
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Writer thread has been stopped!", name);
            shutdownSignal.countDown();
        }
    }

    Optional<Output> filter(Input frame) {
        return Optional.empty();
    }
}
