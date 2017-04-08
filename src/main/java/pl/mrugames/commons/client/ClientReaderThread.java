package pl.mrugames.commons.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.filters.Filter;
import pl.mrugames.commons.client.filters.FilterProcessor;
import pl.mrugames.commons.client.io.ClientReader;

import java.io.EOFException;
import java.io.Serializable;
import java.net.SocketException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

class ClientReaderThread<Input extends Serializable, Output> implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ClientReaderThread.class);

    private final String name;
    private final BlockingQueue<Output> received;
    private final ClientReader<Input> clientReader;
    private final List<Filter<?, ?>> filters;
    private final FilterProcessor filterProcessor;

    private volatile boolean interrupted;
    private volatile CountDownLatch shutdownSignal;

    ClientReaderThread(String name,
                       BlockingQueue<Output> received,
                       ClientReader<Input> clientReader,
                       List<Filter<?, ?>> filters,
                       FilterProcessor filterProcessor) {
        this.name = name;
        this.received = received;
        this.clientReader = clientReader;
        this.filters = filters;
        this.filterProcessor = filterProcessor;
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
                Input frame = clientReader.next();
                if (frame == null) {
                    break;
                }

                Optional<Output> transformed = filterProcessor.filter(frame, filters);
                if (transformed.isPresent()) {
                    received.add(transformed.get());
                }
            }
        } catch (EOFException | SocketException e) {
            logger.debug("[{}] Connection related exception!", name, e);
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Reader thread has been stopped!", name);
            shutdownSignal.countDown();
        }
    }

}
