package pl.mrugames.nucleus.common.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class ObjectReader<FrameType extends Serializable> implements ClientReader<FrameType> {
    private final ByteReader byteReader;

    public ObjectReader(ByteBuffer byteBuffer) {
        this.byteReader = new ByteReader(byteBuffer);
    }

    public ObjectReader(InputStream inputStream) {
        this.byteReader = new ByteReader(inputStream);
    }

    @Override
    public boolean isReady() throws Exception {
        return byteReader.isReady();
    }

    @Override
    @SuppressWarnings("unchecked")
    public FrameType read() throws Exception {
        byte[] frame = byteReader.read();
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(frame);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream)) {
            return (FrameType) objectInputStream.readUnshared();
        }
    }
}
