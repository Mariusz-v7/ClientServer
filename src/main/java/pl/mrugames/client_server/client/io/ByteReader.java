package pl.mrugames.client_server.client.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteReader implements ClientReader<byte[]> {
    private final BufferedInputStream bufferedInputStream;
    private final DataInputStream dataInputStream;

    public ByteReader(InputStream inputStream) {
        this.bufferedInputStream = new BufferedInputStream(inputStream);
        this.dataInputStream = new DataInputStream(bufferedInputStream);
    }

    @Override
    public boolean isReady() throws IOException {
        if (bufferedInputStream.available() < 4) {
            return false;
        }

        try {
            bufferedInputStream.mark(4);
            int len = dataInputStream.readInt();

            return bufferedInputStream.available() >= len;
        } finally {
            bufferedInputStream.reset();
        }
    }

    @Override
    public byte[] read() throws IOException {
        int len = dataInputStream.readInt();

        byte[] bytes = new byte[len];
        int result = dataInputStream.read(bytes);

        if (result != len) {
            throw new IllegalStateException("Length: " + len + ", bytes read: " + result);
        }

        return bytes;
    }

    @Override
    public void close() throws Exception {
        bufferedInputStream.close();
        dataInputStream.close();
    }

    BufferedInputStream getBufferedInputStream() {
        return bufferedInputStream;
    }

    /**
     * Should be called before reading actual message. It removes length field.
     */
    void clearLen() throws IOException {
        long result = bufferedInputStream.skip(4);
        if (result != 4) {
            throw new IllegalStateException("Failed to skip 4 bytes. Actual bytes skipped: " + result);
        }
    }
}
