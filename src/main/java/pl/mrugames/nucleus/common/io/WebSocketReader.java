package pl.mrugames.nucleus.common.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.server.client.frames.WebSocketFrame;

import java.io.IOException;
import java.nio.ByteBuffer;

public class WebSocketReader implements ClientReader<WebSocketFrame> {
    private final static Logger logger = LoggerFactory.getLogger(WebSocketReader.class);
    private final static int MAX_PAYLOAD_SIZE = 1_000_000;
    private final ByteBuffer byteBuffer;

    private final int maskSize = 4;

    public WebSocketReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isReady() throws Exception {
        if (amountAvailable() < 2) {  // frame type + length byte
            return false;
        }

        try {
            byteBuffer.mark();

            byteBuffer.get(); // frame type - just move position

            byte lengthByte = byteBuffer.get();
            if (!isAvailableToComputeLength(lengthByte)) {
                return false;
            }

            long length = computePayloadLength(lengthByte);

            if (amountAvailable() < maskSize + length) {
                return false;
            }

            return true;
        } finally {
            byteBuffer.reset();
        }
    }

    @Override
    public WebSocketFrame read() throws Exception {
        WebSocketFrame.FrameType frameType = getFrameType();

        byte lengthByte = byteBuffer.get();
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

    private WebSocketFrame.FrameType getFrameType() {
        byte first = byteBuffer.get();
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

        return frameType;
    }

    private long computePayloadLength(byte lengthByte) throws IOException {
        int lenByte = (lengthByte & 0x7F);

        if (lenByte <= 0x7D) {
            return lenByte;
        } else if (lenByte == 0x7E) {
            return Short.toUnsignedLong(byteBuffer.getShort());
        } else if (lenByte == 0x7F) {
            return byteBuffer.getLong();
        }

        throw new IllegalStateException("Unknown length, " + lenByte);
    }

    private boolean isAvailableToComputeLength(byte lengthByte) {
        int lenByte = (lengthByte & 0x7F);

        if (lenByte <= 0x7D) {
            return true;
        } else if (lenByte == 0x7E) {
            return amountAvailable() >= 2;  // short
        } else if (lenByte == 0x7F) {
            return amountAvailable() >= 4; // long
        }

        throw new IllegalStateException("Unknown length, " + lenByte);
    }

    private int amountAvailable() {
        return byteBuffer.limit() - byteBuffer.position();
    }

    private byte[] decode(byte[] mask, int payloadLength) throws IOException {
        byte[] buffer = new byte[payloadLength];

        if (mask != null) {
            for (int i = 0; i < payloadLength; ++i) {
                buffer[i] = (byte) (byteBuffer.get() ^ mask[i % 4]);
            }
        } else {
            for (int i = 0; i < payloadLength; ++i) {
                buffer[i] = byteBuffer.get();
            }
        }

        return buffer;
    }

    private byte[] getMaskingKey(byte lengthByte) throws IOException {
        boolean withMask = (lengthByte & 0x80) != 0;
        if (!withMask) {
            return null;
        }

        byte[] mask = new byte[maskSize];
        for (int i = 0; i < maskSize; ++i) {
            mask[i] = byteBuffer.get();
        }

        return mask;
    }

}
