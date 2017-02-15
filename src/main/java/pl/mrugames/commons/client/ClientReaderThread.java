package pl.mrugames.commons.client;

import pl.mrugames.commons.client.io.ClientReader;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

class ClientReaderThread<FrameType, StreamType extends AutoCloseable> implements Runnable {
    private final InputStream originalInputStream;
    private final BlockingQueue<FrameType> received;
    private final ClientReader<FrameType, StreamType> clientReader;

    ClientReaderThread(InputStream originalInputStream, BlockingQueue<FrameType> received, ClientReader<FrameType, StreamType> clientReader) {
        this.originalInputStream = originalInputStream;
        this.received = received;
        this.clientReader = clientReader;
    }

    @Override
    public void run() {
        try (StreamType inputStream = clientReader.prepare(originalInputStream)) {
            while (!Thread.currentThread().isInterrupted()) {
                received.add(clientReader.next(inputStream));
            }
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }
    }

}
