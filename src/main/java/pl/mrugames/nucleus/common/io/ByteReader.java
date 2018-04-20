package pl.mrugames.nucleus.common.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteReader implements ClientReader<byte[]> {
    private final ByteBuffer byteBuffer;
    private final DataInputStream inputStream;

    public ByteReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.inputStream = null;
    }

    public ByteReader(InputStream inputStream) {
        this.byteBuffer = null;
        this.inputStream = new DataInputStream(inputStream);
    }

    @Override
    public boolean isReady() throws IOException {
        if (byteBuffer == null) {
            throw new UnsupportedOperationException("Operation available only for non-blocking sockets");
        }

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
        if (byteBuffer != null) {
            int len = byteBuffer.getInt();

            byte[] bytes = new byte[len];
            byteBuffer.get(bytes);

            return bytes;
        }

        if (inputStream != null) {
            int length = inputStream.readInt();

            byte[] bytes = new byte[length];
            int read = inputStream.read(bytes);

            if (read != length) {
                throw new IllegalStateException("Expected " + length + " bytes, read: " + read);
            }

            return bytes;
        }

        throw new IllegalStateException();
    }

}
