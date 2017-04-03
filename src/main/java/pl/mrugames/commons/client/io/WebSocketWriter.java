package pl.mrugames.commons.client.io;

import pl.mrugames.commons.client.frames.WebSocketFrame;

import java.io.OutputStream;

public class WebSocketWriter implements ClientWriter<WebSocketFrame> {

    public WebSocketWriter(OutputStream outputStream) {

    }

    @Override
    public void next(WebSocketFrame frameToSend) throws Exception {

    }
}
