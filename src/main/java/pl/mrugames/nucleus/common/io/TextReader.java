package pl.mrugames.nucleus.common.io;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads <b>any</b> text available.
 */
public class TextReader implements ClientReader<String> {
    private final ByteBuffer byteBuffer;
    private final Charset charset = StandardCharsets.UTF_8;

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
        return new String(bytes, charset);
    }
}
