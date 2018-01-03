package pl.mrugames.client_server.client.io;

import pl.mrugames.client_server.client.frames.WebSocketFrame;

import java.nio.ByteBuffer;

public class WebSocketWriter implements ClientWriter<WebSocketFrame> {
    private final ByteBuffer byteBuffer;

    public WebSocketWriter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void write(WebSocketFrame frameToSend) throws Exception {
        switch (frameToSend.getFrameType()) {
            case CLOSE:
                byteBuffer.put((byte) 0x88);
                byteBuffer.put((byte) 0x00);
                break;
            case TEXT:
                byteBuffer.put((byte) 0x81); // fin + text frame

                long payloadLen = frameToSend.getPayload().length;
                if (payloadLen <= 0x7D) {
                    byteBuffer.putInt((int) (payloadLen & 0x7F));
                } else if (payloadLen <= 0xFFFF) {
                    byteBuffer.put((byte) 0x7E);
                    byteBuffer.putShort((short) payloadLen);
                } else {
                    byteBuffer.put((byte) 0x7F);
                    byteBuffer.putLong(payloadLen);
                }

                byteBuffer.put(frameToSend.getPayload());
                break;
            default:
                throw new UnsupportedOperationException("Only text frames are supported");
        }
    }
}
