package pl.mrugames.client_server.client.io;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class InputBuffer implements AutoCloseable {
    private final BufferedInputStream bufferedInputStream;

    public InputBuffer(InputStream inputStream) {
        this.bufferedInputStream = new BufferedInputStream(inputStream);

    }

    @Override
    public void close() throws Exception {
        bufferedInputStream.close();
    }
}
