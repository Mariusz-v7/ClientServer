package pl.mrugames.client_server.client;

import com.codahale.metrics.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.filters.Filter;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.Serializable;
import java.util.List;
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
    private final List<Filter<?, ?>> filters;
    private final FilterProcessor filterProcessor;
    private final Counter activeCounter;

    private volatile boolean interrupted;
    private volatile CountDownLatch shutdownSignal;

    ClientWriterThread(String name,
                       BlockingQueue<Input> toSend,
                       ClientWriter<Output> clientWriter,
                       long timeout,
                       TimeUnit timeoutUnit,
                       List<Filter<?, ?>> filters,
                       FilterProcessor filterProcessor,
                       Counter activeCounter) {
        this.name = name;
        this.toSend = toSend;
        this.clientWriter = clientWriter;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.filters = filters;
        this.filterProcessor = filterProcessor;
        this.activeCounter = activeCounter;
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
        activeCounter.inc();
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
                    logger.debug("[{}] sending fame: {}", name, frame);
                    Optional<Output> transformed = filterProcessor.filter(frame, filters);
                    if (transformed.isPresent()) {
                        logger.debug("[{}] transformed fame: {}", name, transformed.get());
                        clientWriter.next(transformed.get());
                    } else {
                        logger.debug("[{}] frame transformed to null", name);
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
            activeCounter.dec();
        }
    }
}
