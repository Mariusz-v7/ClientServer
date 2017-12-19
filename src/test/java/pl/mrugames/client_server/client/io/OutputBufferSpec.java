package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.SocketHelper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OutputBufferSpec {
    private SocketHelper socketHelper;
    private InputBuffer inputBuffer;
    private OutputBuffer outputBuffer;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        inputBuffer = new InputBuffer(socketHelper.getInputStream());
        outputBuffer = new OutputBuffer(socketHelper.getOutputStream());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
        inputBuffer.close();
        outputBuffer.close();
    }

    @Test
    void writeReadTest() throws IOException {
        outputBuffer.write((byte) 1, (byte) 2, (byte) 3);
        outputBuffer.write((byte) 3, (byte) 4, (byte) 5, (byte) 6);

        byte[] bytes = inputBuffer.read();
        assertThat(bytes).containsExactly(1, 2, 3);

        bytes = inputBuffer.read();
        assertThat(bytes).containsExactly(3, 4, 5, 6);
    }

}
