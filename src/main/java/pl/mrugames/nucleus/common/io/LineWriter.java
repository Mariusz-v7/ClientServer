package pl.mrugames.nucleus.common.io;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LineWriter implements ClientWriter<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;
    private final String lineEnding = "\r\n";
    private final OutputStream outputStream;

    public LineWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.outputStream = null;
    }

    public LineWriter(OutputStream outputStream) {
        this.byteBuffer = null;
        this.outputStream = outputStream;
    }

    @Override
    public void write(String frameToSend) throws Exception {
        frameToSend += lineEnding;

        if (byteBuffer != null) {
            byteBuffer.put(frameToSend.getBytes(charset));
        }

        if (outputStream != null) {
            outputStream.write(frameToSend.getBytes(charset));
        }
    }

    public String getLineEnding() {
        return lineEnding;
    }
}
