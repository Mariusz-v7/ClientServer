package pl.mrugames.commons.client.io;

import pl.mrugames.commons.client.IOExceptionWrapper;

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
    @SuppressWarnings("unchecked")
    // well.. other side may send data which is not instance of a FrameType, but we cannot do anything about it but fail the client connection...
    public FrameType next() throws Exception {
        return (FrameType) stream.readUnshared();
    }
}
