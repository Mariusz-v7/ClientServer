package pl.mrugames.client_server.client.filters;

import pl.mrugames.client_server.client.frames.WebSocketFrame;
import pl.mrugames.client_server.websocket.WebsocketConstants;

import javax.annotation.Nullable;

public class StringToWebSocketFrameFilter implements Filter<String, WebSocketFrame> {
    private static volatile StringToWebSocketFrameFilter instance;

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

        if (s.equals(WebsocketConstants.WEBSOCKET_CLOSE_FRAME)) {
            return new WebSocketFrame(WebSocketFrame.FrameType.CLOSE, new byte[0]);
        }

        return new WebSocketFrame(WebSocketFrame.FrameType.TEXT, s.getBytes());
    }
}
