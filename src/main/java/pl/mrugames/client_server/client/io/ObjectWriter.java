package pl.mrugames.client_server.client.io;

import pl.mrugames.client_server.client.IOExceptionWrapper;

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
    public void write(FrameType frameToSend) throws Exception {
        stream.writeUnshared(frameToSend);
        stream.flush();
        stream.reset();
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }
}
