package pl.mrugames.client_server.client.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OutputBuffer implements AutoCloseable {
    private final BufferedOutputStream bufferedOutputStream;
    private final DataOutputStream dataOutputStream;

    public OutputBuffer(OutputStream outputStream) {
        bufferedOutputStream = new BufferedOutputStream(outputStream);
        dataOutputStream = new DataOutputStream(bufferedOutputStream);
    }

    void write(byte... bytes) throws IOException {
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        dataOutputStream.flush();
    }

    @Override
    public void close() throws Exception {
        bufferedOutputStream.close();
        dataOutputStream.close();
    }
}
