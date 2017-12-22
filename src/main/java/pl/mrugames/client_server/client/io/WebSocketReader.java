package pl.mrugames.client_server.client.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.frames.WebSocketFrame;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WebSocketReader implements ClientReader<WebSocketFrame> {
    private final static Logger logger = LoggerFactory.getLogger(WebSocketReader.class);
    private final static int MAX_PAYLOAD_SIZE = 1_000_000;
    private final DataInputStream stream;

    public WebSocketReader(InputStream inputStream) {
        stream = new DataInputStream(new BufferedInputStream(inputStream));
    }

    @Override
    public boolean isReady() throws Exception {
        return false; // TODO
    }

    @Override
    public WebSocketFrame read() throws Exception {

        byte first = (byte) stream.read();
        boolean fin = (first & 0x80) != 0;
        if (!fin) {
            throw new UnsupportedOperationException("Message fragmentation not supported");
        }

        if ((first & 0x70) != 0) {
            logger.warn("Reserved bits should be 0, but were {}. Returning null.", first & 0x70);
            return null;
        }

        byte opcode = (byte) (first & 0x0F);
        WebSocketFrame.FrameType frameType;

        switch (opcode) {
            case 0x00:
                throw new UnsupportedOperationException("Message fragmentation not supported");
            case 0x01:
                frameType = WebSocketFrame.FrameType.TEXT;
                break;
            case 0x02:
                frameType = WebSocketFrame.FrameType.BINARY;
                break;
            case 0x08:
                frameType = WebSocketFrame.FrameType.CLOSE;
                break;
            case 0x09:
                throw new UnsupportedOperationException("ping not supported");
            case 0x0A:
                throw new UnsupportedOperationException("pong not supported");
            default:
                throw new IllegalStateException("Unsupported opcode received");
        }

        byte lengthByte = (byte) stream.read();
        long payloadLength = computePayloadLength(lengthByte);

        if (payloadLength < 0) {
            throw new IllegalStateException("Failed to read payload length");
        }

        if (payloadLength > MAX_PAYLOAD_SIZE) {
            throw new IllegalArgumentException(String.format("Payload length exceeds maximum allowed size. Payload length: %d", payloadLength));
        }

        byte[] mask = getMaskingKey(lengthByte);
        byte[] decoded = decode(mask, (int) payloadLength);

        return new WebSocketFrame(frameType, decoded);
    }

    private long computePayloadLength(byte lengthByte) throws IOException {
        int lenByte = (lengthByte & 0x7F);

        if (lenByte <= 0x7D)
            return lenByte;
        else if (lenByte == 0x7E) {
            return Short.toUnsignedLong(stream.readShort());
        } else if (lenByte == 0x7F) {
            return stream.readLong();
        }

        return -1;
    }

    private byte[] decode(byte[] mask, int payloadLength) throws IOException {
        byte[] buffer = new byte[payloadLength];

        if (mask != null) {
            for (int i = 0; i < payloadLength; ++i) {
                buffer[i] = (byte) (stream.read() ^ mask[i % 4]);
            }
        } else {
            for (int i = 0; i < payloadLength; ++i) {
                buffer[i] = (byte) stream.read();
            }
        }

        return buffer;
    }

    private byte[] getMaskingKey(byte lengthByte) throws IOException {
        boolean withMask = (lengthByte & 0x80) != 0;
        if (!withMask) {
            return null;
        }

        int maskSize = 4;
        byte[] mask = new byte[maskSize];
        for (int i = 0; i < maskSize; ++i) {
            mask[i] = (byte) stream.read();
        }
        return mask;
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }
}
