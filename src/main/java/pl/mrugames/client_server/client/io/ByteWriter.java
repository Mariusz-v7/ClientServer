package pl.mrugames.client_server.client.io;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteWriter {
    private final ByteBuffer byteBuffer;

    public ByteWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    void write(byte... bytes) throws IOException {
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
    }
}
