package pl.mrugames.client_server.client.frames;

import java.io.Serializable;
import java.util.Arrays;

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

    public FrameType getFrameType() {
        return frameType;
    }

    @Override
    public String toString() {
        return "WebSocketFrame{" +
                "payload=" + Arrays.toString(payload) +
                ", frameType=" + frameType +
                '}';
    }
}
