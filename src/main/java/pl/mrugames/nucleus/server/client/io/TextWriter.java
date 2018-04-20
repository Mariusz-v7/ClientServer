package pl.mrugames.nucleus.server.client.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TextWriter implements ClientWriter<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;

    public TextWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void write(String frameToSend) throws Exception {
        byteBuffer.put(frameToSend.getBytes(charset));
    }
}
