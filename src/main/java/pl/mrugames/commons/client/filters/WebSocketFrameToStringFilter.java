package pl.mrugames.commons.client.filters;

import pl.mrugames.commons.client.frames.WebSocketFrame;

import javax.annotation.Nullable;

public class WebSocketFrameToStringFilter implements Filter<WebSocketFrame, String> {
    private static WebSocketFrameToStringFilter instance;

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

        return new String(webSocketFrame.getPayload());
    }
}
