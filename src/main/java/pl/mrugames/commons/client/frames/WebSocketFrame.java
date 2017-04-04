package pl.mrugames.commons.client.frames;

import java.io.Serializable;

public class WebSocketFrame implements Serializable {
    public enum FrameType {
        TEXT, BINARY, CLOSE
    }

    private final byte[] payload;
    private final FrameType frameType;

    public WebSocketFrame(FrameType frameType, byte[] payload) {
        this.frameType = frameType;
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }
}
