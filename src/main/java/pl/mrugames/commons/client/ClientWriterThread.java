package pl.mrugames.commons.client;

import pl.mrugames.commons.client.io.ClientWriter;

import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ClientWriterThread<FrameType, StreamType extends AutoCloseable> implements Runnable {
    private final OutputStream originalOutputStream;
    private final BlockingQueue<FrameType> toSend;
    private final ClientWriter<FrameType, StreamType> clientWriter;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    public ClientWriterThread(OutputStream originalOutputStream, BlockingQueue<FrameType> toSend, ClientWriter<FrameType, StreamType> clientWriter, long timeout, TimeUnit timeoutUnit) {
        this.originalOutputStream = originalOutputStream;
        this.toSend = toSend;
        this.clientWriter = clientWriter;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public void run() {
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
        }
    }
}
