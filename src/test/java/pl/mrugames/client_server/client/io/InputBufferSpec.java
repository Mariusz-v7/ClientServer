package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.SocketHelper;

import java.io.DataOutputStream;
import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class InputBufferSpec {
    private InputBuffer inputBuffer;
    private DataOutputStream dataOutputStream;

    private SocketHelper socketHelper;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();
        dataOutputStream = new DataOutputStream(socketHelper.getOutputStream());

        inputBuffer = new InputBuffer(socketHelper.getInputStream());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void givenNothingSent_whenCheckReady_thenFalse() throws IOException {
        assertThat(inputBuffer.isReady()).isFalse();
    }

    @Test
    void givenLenFieldNotComplete_whenCheckReady_thenFalse() throws IOException {
        dataOutputStream.writeByte(1);  // one byte means only 1/4 of len field
        assertThat(inputBuffer.isReady()).isFalse();
    }

    @Test
    void givenLenIs4_andOnly2Sent_whenCheckReady_thenFalse() throws IOException {
        dataOutputStream.writeInt(4);
        assertThat(inputBuffer.isReady()).isFalse();
    }

    @Test
    void givenLenIs4_andFullMessageSent_whenCheckReady_thenTrue() throws IOException {
        dataOutputStream.writeInt(4);

        dataOutputStream.writeInt(1);
        assertThat(inputBuffer.isReady()).isTrue();
    }

    @Test
    void givenFullMessageArrivesInTwoSteps_whenCheckReadyTwice_firstIsFalseSecondIsTrue() throws IOException {
        dataOutputStream.writeInt(4);

        dataOutputStream.writeShort(3);
        assertThat(inputBuffer.isReady()).isFalse();

        dataOutputStream.writeShort(3);
        assertThat(inputBuffer.isReady()).isTrue();
    }

    @Test
    void readTest() throws IOException {
        dataOutputStream.writeInt(4); // len

        assertThat(inputBuffer.isReady()).isFalse();

        dataOutputStream.writeInt(0);
        assertThat(inputBuffer.isReady()).isTrue();

        inputBuffer.clearLen();
        inputBuffer.getBufferedInputStream().read(new byte[4]);

        assertThat(inputBuffer.isReady()).isFalse();

        // second message

        dataOutputStream.writeInt(2); // len
        assertThat(inputBuffer.isReady()).isFalse();

        dataOutputStream.writeShort(1);
        assertThat(inputBuffer.isReady()).isTrue();

        inputBuffer.clearLen();
        inputBuffer.getBufferedInputStream().read(new byte[2]);

        assertThat(inputBuffer.isReady()).isFalse();

        // third message
        dataOutputStream.writeShort(0);  //len 1/2
        assertThat(inputBuffer.isReady()).isFalse();
        dataOutputStream.writeShort(1); // len 1/2
        assertThat(inputBuffer.isReady()).isFalse();

        dataOutputStream.writeByte(1);

        assertThat(inputBuffer.isReady()).isTrue();
    }

    @Test
    void multiplePacketsTest() throws IOException {
        dataOutputStream.writeInt(4); // len
        assertThat(inputBuffer.isReady()).isFalse();

        dataOutputStream.writeInt(0);
        assertThat(inputBuffer.isReady()).isTrue();

        dataOutputStream.writeInt(4); // len
        assertThat(inputBuffer.isReady()).isTrue();

        dataOutputStream.writeInt(0);
        assertThat(inputBuffer.isReady()).isTrue();

        inputBuffer.clearLen();
        inputBuffer.getBufferedInputStream().read(new byte[4]);
        assertThat(inputBuffer.isReady()).isTrue();

        dataOutputStream.writeInt(4); // len
        assertThat(inputBuffer.isReady()).isTrue();

        dataOutputStream.writeInt(0);
        assertThat(inputBuffer.isReady()).isTrue();

        inputBuffer.clearLen();
        inputBuffer.getBufferedInputStream().read(new byte[4]);
        assertThat(inputBuffer.isReady()).isTrue();

        inputBuffer.clearLen();
        inputBuffer.getBufferedInputStream().read(new byte[4]);
        assertThat(inputBuffer.isReady()).isFalse();

    }

}
