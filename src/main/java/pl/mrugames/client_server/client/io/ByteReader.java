package pl.mrugames.client_server.client.io;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteReader implements ClientReader<byte[]> {
    private final ByteBuffer byteBuffer;

    public ByteReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isReady() throws IOException {
        if (!byteBuffer.hasRemaining()) {
            return false;
        }

        if (byteBuffer.limit() - byteBuffer.position() < 4) {
            return false;
        }

        try {
            byteBuffer.mark();
            int len = byteBuffer.getInt();

            return byteBuffer.limit() - byteBuffer.position() >= len;
        } finally {
            byteBuffer.reset();
        }
    }

    @Override
    public byte[] read() throws IOException {
        int len = byteBuffer.getInt();

        byte[] bytes = new byte[len];
        byteBuffer.get(bytes);

        return bytes;
    }

}
