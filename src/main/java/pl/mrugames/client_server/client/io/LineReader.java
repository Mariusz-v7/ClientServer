package pl.mrugames.client_server.client.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LineReader implements ClientReader<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;

    LineReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isReady() throws Exception {
        byteBuffer.mark();

        int available = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[available];
        byteBuffer.get(bytes);
        String str = new String(bytes, charset);

        byteBuffer.reset();

        return str.contains("\n");
    }

    @Override
    public String read() throws Exception {
        byteBuffer.mark();

        int available = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[available];
        byteBuffer.get(bytes);
        String str = new String(bytes, charset);
        int firstNl = str.indexOf("\n");

        str = str.substring(0, firstNl + 1);

        byteBuffer.reset();

        byteBuffer.position(byteBuffer.position() + str.getBytes(charset).length);

        str = str.substring(0, str.length() - 1); // remove nL

        return str;
    }

    @Override
    public void close() throws Exception {

    }
}
