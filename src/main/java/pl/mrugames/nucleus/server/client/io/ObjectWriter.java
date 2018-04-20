package pl.mrugames.nucleus.server.client.io;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class ObjectWriter<FrameType extends Serializable> implements ClientWriter<FrameType> {
    private final ByteWriter byteWriter;

    public ObjectWriter(ByteBuffer byteBuffer) {
        this.byteWriter = new ByteWriter(byteBuffer);
    }

    @Override
    public void write(FrameType frameToSend) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(frameToSend);

            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteWriter.write(bytes);
        }
    }
}
