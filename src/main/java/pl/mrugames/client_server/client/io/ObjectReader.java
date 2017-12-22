package pl.mrugames.client_server.client.io;

import pl.mrugames.client_server.client.IOExceptionWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class ObjectReader<FrameType extends Serializable> implements ClientReader<FrameType> {
    private final ObjectInputStream stream;

    public ObjectReader(InputStream stream) {
        try {
            this.stream = new ObjectInputStream(stream);
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    }

    @Override
    public boolean isReady() throws Exception {
        return false; // TODO
    }

    @Override
    @SuppressWarnings("unchecked")
    // well.. other side may send data which is not instance of a FrameType, but we cannot do anything about it but fail the client connection...
    public FrameType read() throws Exception {
        return (FrameType) stream.readUnshared();
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }
}
