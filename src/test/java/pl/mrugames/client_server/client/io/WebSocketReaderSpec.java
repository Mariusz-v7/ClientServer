package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.frames.WebSocketFrame;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketReaderSpec {
    private ByteBuffer byteBuffer;
    private WebSocketReader reader;

    private void mockReader(int... bytes) {
        byteBuffer = ByteBuffer.allocate(bytes.length);
        for (int b : bytes) {
            byteBuffer.put((byte) b);
        }

        byteBuffer.flip();
        reader = new WebSocketReader(byteBuffer);
    }

    @Test
    void givenFrameWithStringHello_thenDecodeProperly() throws Exception {
        mockReader(0x81, 0x85, 0xB5, 0x9C, 0x1E, 0x93, 0xDD, 0xF9, 0x72, 0xFF, 0xDA);
        assertThat(reader.isReady()).isTrue();

        WebSocketFrame frame = reader.read();
        assertThat(frame.getPayload()).containsExactly(0x68, 0x65, 0x6C, 0x6C, 0x6F);

        assertThat(reader.isReady()).isFalse();
    }

    @Test
    void givenTwoFramesAreSentAtTheSameTime_thenShouldRecognizeAndReturnTwoFrames() throws Exception {
        mockReader(
                0x81, 0x85, 0xB5, 0x9C, 0x1E, 0x93, 0xDD, 0xF9, 0x72, 0xFF, 0xDA,
                0x81, 0x85, 0xB5, 0x9C, 0x1E, 0x93, 0xDD, 0xF9, 0x72, 0xFF, 0xDA
        );

        assertThat(reader.isReady()).isTrue();
        WebSocketFrame frame1 = reader.read();

        assertThat(reader.isReady()).isTrue();
        WebSocketFrame frame2 = reader.read();

        assertThat(frame1.getPayload()).containsExactly(0x68, 0x65, 0x6C, 0x6C, 0x6F);
        assertThat(frame2.getPayload()).containsExactly(0x68, 0x65, 0x6C, 0x6C, 0x6F);

        assertThat(reader.isReady()).isFalse();
    }

}
