package pl.mrugames.client_server.client.io;

import pl.mrugames.client_server.client.frames.WebSocketFrame;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;

public class WebSocketWriter implements ClientWriter<WebSocketFrame> {
    private final DataOutputStream stream;

    public WebSocketWriter(OutputStream outputStream) {
        stream = new DataOutputStream(new BufferedOutputStream(outputStream));
    }

    @Override
    public void write(WebSocketFrame frameToSend) throws Exception {
        switch (frameToSend.getFrameType()) {
            case CLOSE:
                stream.write(0x88);
                stream.write(0x00);
                break;
            case TEXT:
                stream.write(0x81); // fin + text frame

                long payloadLen = frameToSend.getPayload().length;
                if (payloadLen <= 0x7D) {
                    stream.write((int) (payloadLen & 0x7F));
                } else if (payloadLen <= 0xFFFF) {
                    stream.write(0x7E);
                    stream.writeShort((int) payloadLen);
                } else {
                    stream.write(0x7F);
                    stream.writeLong(payloadLen);
                }

                stream.write(frameToSend.getPayload());
                break;
            default:
                throw new UnsupportedOperationException("Only text frames are supported");
        }

        stream.flush();
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }
}
