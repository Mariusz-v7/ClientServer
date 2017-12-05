package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.Serializable;
import java.time.Instant;

public class CommV2<In extends Serializable, Out extends Serializable> {
    private final ClientWriter<Out> clientWriter;
    private final ClientReader<In> clientReader;
    private volatile Instant lastDataSent;
    private volatile Instant lastDataReceived;

    CommV2(ClientWriter<Out> clientWriter, ClientReader<In> clientReader) {
        this.clientWriter = clientWriter;
        this.clientReader = clientReader;

        Instant now = Instant.now();

        this.lastDataReceived = now;
        this.lastDataSent = now;
    }

    public synchronized void send(Out frame) throws Exception {
        clientWriter.next(frame);
        lastDataSent = Instant.now();
    }

    public synchronized In receive() throws Exception {
        In in = clientReader.next();
        lastDataReceived = Instant.now();
        return in;
    }

    Instant getLastDataSent() {
        return lastDataSent;
    }

    Instant getLastDataReceived() {
        return lastDataReceived;
    }
}
