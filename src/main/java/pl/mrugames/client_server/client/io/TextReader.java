package pl.mrugames.client_server.client.io;

import java.nio.ByteBuffer;

/**
 * Reads <b>any</b> text available.
 */
public class TextReader implements ClientReader<String> {
    private final ByteBuffer byteBuffer;

    public TextReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isReady() throws Exception {
        return byteBuffer.hasRemaining();
    }

    @Override
    public String read() throws Exception {
        int available = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[available];
        byteBuffer.get(bytes);
        return new String(bytes);
    }

    @Override
    @Deprecated
    public void close() throws Exception {
    }
}
