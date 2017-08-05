package pl.mrugames.client_server.client;

import com.codahale.metrics.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.filters.Filter;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;

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
    private final Counter activeCounter;

    private volatile boolean interrupted;
    private volatile CountDownLatch shutdownSignal;

    ClientReaderThread(String name,
                       BlockingQueue<Output> received,
                       ClientReader<Input> clientReader,
                       List<Filter<?, ?>> filters,
                       FilterProcessor filterProcessor,
                       Counter activeCounter) {
        this.name = name;
        this.received = received;
        this.clientReader = clientReader;
        this.filters = filters;
        this.filterProcessor = filterProcessor;
        this.activeCounter = activeCounter;
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
        activeCounter.inc();
        shutdownSignal = new CountDownLatch(1);
        logger.info("[{}] Reader thread started!", name);

        try {
            while (!interrupted && !Thread.currentThread().isInterrupted()) {
                Input frame = clientReader.next();
                if (frame == null) {
                    logger.debug("[{}] received null, closing: ", name);
                    break;
                }

                logger.debug("[{}] received frame: {}", name, frame);
                Optional<Output> transformed = filterProcessor.filter(frame, filters);
                if (transformed.isPresent()) {
                    logger.debug("[{}] received transformed frame: {}", name, transformed.get());
                    received.add(transformed.get());
                } else {
                    logger.debug("[{}] frame transformed to null", name);
                }
            }
        } catch (EOFException | SocketException e) {
            logger.debug("[{}] Connection related exception!", name, e);
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        } finally {
            logger.info("[{}] Reader thread has been stopped!", name);
            shutdownSignal.countDown();
            activeCounter.dec();
        }
    }

}
