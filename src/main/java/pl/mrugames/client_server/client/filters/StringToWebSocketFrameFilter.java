package pl.mrugames.client_server.client.filters;

import pl.mrugames.client_server.client.frames.WebSocketFrame;

import javax.annotation.Nullable;

public class StringToWebSocketFrameFilter implements Filter<String, WebSocketFrame> {
    private static StringToWebSocketFrameFilter instance;

    public static synchronized StringToWebSocketFrameFilter getInstance() {
        if (instance == null) {
            instance = new StringToWebSocketFrameFilter();
        }

        return instance;
    }

    private StringToWebSocketFrameFilter() {
    }

    @Override
    public WebSocketFrame filter(@Nullable String s) {
        if (s == null) {
            return null;
        }

        return new WebSocketFrame(WebSocketFrame.FrameType.TEXT, s.getBytes());
    }
}
