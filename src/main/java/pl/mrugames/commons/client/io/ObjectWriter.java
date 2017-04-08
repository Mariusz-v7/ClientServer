package pl.mrugames.commons.client.io;

import pl.mrugames.commons.client.IOExceptionWrapper;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class ObjectWriter<FrameType extends Serializable> implements ClientWriter<FrameType> {
    private final ObjectOutputStream stream;

    public ObjectWriter(OutputStream stream) {
        try {
            this.stream = new ObjectOutputStream(stream);
        } catch (IOException e) {
            throw new IOExceptionWrapper(e);
        }
    }

    @Override
    public void next(FrameType frameToSend) throws Exception {
        stream.writeUnshared(frameToSend);
        stream.flush();
        stream.reset();
    }
}
