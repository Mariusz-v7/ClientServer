package pl.mrugames.nucleus.common.io;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LineReader implements ClientReader<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;
    private final String lineEnding = "\r\n";
    private final InputStream inputStream;

    public LineReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.inputStream = null;
    }

    public LineReader(InputStream inputStream) {
        this.byteBuffer = null;
        this.inputStream = inputStream;
    }

    @Override
    public boolean isReady() throws Exception {
        if (byteBuffer == null) {
            throw new UnsupportedOperationException("Operation available only for non-blocking sockets");
        }

        byteBuffer.mark();

        int available = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[available];
        byteBuffer.get(bytes);
        String str = new String(bytes, charset);

        byteBuffer.reset();

        return str.contains(lineEnding);
    }

    @Override
    public String read() throws Exception {
        if (byteBuffer != null) {
            byteBuffer.mark();

            int available = byteBuffer.limit() - byteBuffer.position();
            byte[] bytes = new byte[available];
            byteBuffer.get(bytes);
            String str = new String(bytes, charset);
            int firstNl = str.indexOf(lineEnding);

            str = str.substring(0, firstNl);

            byteBuffer.reset();

            byteBuffer.position(byteBuffer.position() + str.getBytes(charset).length + lineEnding.getBytes(charset).length);

            str = str.substring(0, str.length());

            return str;
        }

        if (inputStream != null) {
            Scanner scanner = new Scanner(inputStream).useDelimiter(lineEnding);
            return scanner.next() + lineEnding;
        }

        throw new IllegalStateException();
    }

    public String getLineEnding() {
        return lineEnding;
    }
}
