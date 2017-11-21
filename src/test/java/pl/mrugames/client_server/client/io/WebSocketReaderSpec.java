package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.frames.WebSocketFrame;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

class WebSocketReaderSpec {
    private InputStream inputStream;
    private WebSocketReader reader;

    private void mockReader(int... bytes) {
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            b[i] = (byte) bytes[i];
        }

        inputStream = spy(new ByteArrayInputStream(b));
        reader = new WebSocketReader(inputStream);
    }

    @Test
    void givenFrameWithStringHello_thenDecodeProperly() throws Exception {
        mockReader(0x81, 0x85, 0xB5, 0x9C, 0x1E, 0x93, 0xDD, 0xF9, 0x72, 0xFF, 0xDA);
        WebSocketFrame frame = reader.next();
        assertThat(frame.getPayload()).containsExactly(0x68, 0x65, 0x6C, 0x6C, 0x6F);
    }

    @Test
    void givenTwoFramesAreSentAtTheSameTime_thenShouldRecognizeAndReturnTwoFrames() throws Exception {
        mockReader(
                0x81, 0x85, 0xB5, 0x9C, 0x1E, 0x93, 0xDD, 0xF9, 0x72, 0xFF, 0xDA,
                0x81, 0x85, 0xB5, 0x9C, 0x1E, 0x93, 0xDD, 0xF9, 0x72, 0xFF, 0xDA
        );
        WebSocketFrame frame1 = reader.next();
        WebSocketFrame frame2 = reader.next();
        assertThat(frame1.getPayload()).containsExactly(0x68, 0x65, 0x6C, 0x6C, 0x6F);
        assertThat(frame2.getPayload()).containsExactly(0x68, 0x65, 0x6C, 0x6C, 0x6F);

    }

}
