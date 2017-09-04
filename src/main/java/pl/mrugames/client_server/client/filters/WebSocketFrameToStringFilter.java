package pl.mrugames.client_server.client.filters;

import pl.mrugames.client_server.client.frames.WebSocketFrame;
import pl.mrugames.client_server.websocket.WebsocketConstatns;

import javax.annotation.Nullable;

public class WebSocketFrameToStringFilter implements Filter<WebSocketFrame, String> {
    private static volatile WebSocketFrameToStringFilter instance;

    public synchronized static WebSocketFrameToStringFilter getInstance() {
        if (instance == null) {
            instance = new WebSocketFrameToStringFilter();
        }

        return instance;
    }

    private WebSocketFrameToStringFilter() {
    }

    @Override
    public String filter(@Nullable WebSocketFrame webSocketFrame) {
        if (webSocketFrame == null) {
            return null;
        }

        if (webSocketFrame.getFrameType() == WebSocketFrame.FrameType.CLOSE) {
            return WebsocketConstatns.WEBSOCKET_CLOSE_FRAME;
        }

        return new String(webSocketFrame.getPayload());
    }
}
