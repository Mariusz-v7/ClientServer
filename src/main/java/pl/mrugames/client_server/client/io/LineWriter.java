package pl.mrugames.client_server.client.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LineWriter implements ClientWriter<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;

    LineWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void write(String frameToSend) throws Exception {
        frameToSend += "\n";
        byteBuffer.put(frameToSend.getBytes(charset));
    }

    @Override
    public void close() throws Exception {

    }
}
