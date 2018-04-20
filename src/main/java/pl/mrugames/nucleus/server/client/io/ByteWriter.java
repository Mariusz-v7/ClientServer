package pl.mrugames.nucleus.server.client.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteWriter implements ClientWriter<byte[]> {
    private final ByteBuffer byteBuffer;
    private final DataOutputStream outputStream;

    public ByteWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.outputStream = null;
    }

    public ByteWriter(OutputStream outputStream) {
        this.byteBuffer = null;
        this.outputStream = new DataOutputStream(outputStream);
    }

    @Override
    public void write(byte... bytes) throws IOException {
        if (byteBuffer != null) {
            byteBuffer.putInt(bytes.length);
            byteBuffer.put(bytes);
        }

        if (outputStream != null) {
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
        }
    }

}
