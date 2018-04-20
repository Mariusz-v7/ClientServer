package pl.mrugames.nucleus.server.client.io;

import java.io.*;
import java.nio.ByteBuffer;

public class ObjectWriter<FrameType extends Serializable> implements ClientWriter<FrameType> {
    private final ByteWriter byteWriter;

    public ObjectWriter(ByteBuffer byteBuffer) {
        this.byteWriter = new ByteWriter(byteBuffer);
    }

    public ObjectWriter(OutputStream outputStream) {
        this.byteWriter = new ByteWriter(outputStream);
    }

    @Override
    public void write(FrameType frameToSend) throws Exception {
        byte[] bytes = bytes(frameToSend);
        byteWriter.write(bytes);
    }

    private byte[] bytes(FrameType frameToSend) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(frameToSend);

            return byteArrayOutputStream.toByteArray();
        }
    }
}
