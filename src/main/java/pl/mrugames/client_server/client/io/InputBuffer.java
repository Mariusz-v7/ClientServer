package pl.mrugames.client_server.client.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputBuffer implements AutoCloseable {
    private final BufferedInputStream bufferedInputStream;
    private final DataInputStream dataInputStream;

    public InputBuffer(InputStream inputStream) {
        this.bufferedInputStream = new BufferedInputStream(inputStream);
        this.dataInputStream = new DataInputStream(bufferedInputStream);
    }

    boolean isReady() throws IOException {
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

    BufferedInputStream getBufferedInputStream() {
        return bufferedInputStream;
    }

    /**
     * Should be called before reading actual message. It removes length field.
     */
    void clearLen() throws IOException {
        bufferedInputStream.skip(4);
    }

    @Override
    public void close() throws Exception {
        bufferedInputStream.close();
        dataInputStream.close();
    }
}
