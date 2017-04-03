package pl.mrugames.commons.client.io;

import pl.mrugames.commons.client.frames.WebSocketFrame;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class WebSocketReader implements ClientReader<WebSocketFrame> {
    private final BufferedInputStream bufferedInputStream;

    public WebSocketReader(InputStream inputStream) {
        bufferedInputStream = new BufferedInputStream(inputStream);
    }

    @Override
    public WebSocketFrame next() throws Exception {

        return null;
    }
}
