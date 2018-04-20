package pl.mrugames.nucleus.common.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LineWriter implements ClientWriter<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;
    private final String lineEnding = "\r\n";

    public LineWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void write(String frameToSend) throws Exception {
        frameToSend += lineEnding;
        byteBuffer.put(frameToSend.getBytes(charset));
    }

    public String getLineEnding() {
        return lineEnding;
    }
}
