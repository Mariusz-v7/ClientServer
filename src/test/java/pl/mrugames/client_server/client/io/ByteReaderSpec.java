package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.SocketHelper;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class ByteReaderSpec {
    private ByteReader byteReader;

    private SocketHelper socketHelper;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        byteReader = new ByteReader(socketHelper.getReadBuffer());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void givenNothingSent_whenCheckReady_thenFalse() throws IOException {
        assertThat(byteReader.isReady()).isFalse();
    }

    @Test
    void givenLenFieldNotComplete_whenCheckReady_thenFalse() throws IOException {
        socketHelper.write((byte) 1);  // one byte means only 1/4 of len field
        assertThat(byteReader.isReady()).isFalse();
    }

    @Test
    void givenLenIs4_andOnly2Sent_whenCheckReady_thenFalse() throws IOException {
        socketHelper.writeInt(4);
        socketHelper.writeShort((short) 1);
        assertThat(byteReader.isReady()).isFalse();
    }

    @Test
    void givenLenIs4_andFullMessageSent_whenCheckReady_thenTrue() throws IOException {
        socketHelper.writeInt(4);
        socketHelper.writeInt(1);

        assertThat(byteReader.isReady()).isTrue();
    }

    @Test
    void givenFullMessageArrivesInTwoSteps_whenCheckReadyTwice_firstIsFalseSecondIsTrue() throws IOException {
        socketHelper.writeInt(4);

        socketHelper.writeShort((short) 3);
        assertThat(byteReader.isReady()).isFalse();

        socketHelper.writeShort((short) 3);
        assertThat(byteReader.isReady()).isTrue();
    }

    @Test
    void readTest() throws IOException {
        socketHelper.writeInt(4); // len

        assertThat(byteReader.isReady()).isFalse();

        socketHelper.writeInt(0);
        assertThat(byteReader.isReady()).isTrue();

        byte[] bytes = byteReader.read();
        assertThat(bytes).hasSize(4);

        assertThat(byteReader.isReady()).isFalse();

        // second message

        socketHelper.writeInt(2); // len
        assertThat(byteReader.isReady()).isFalse();

        socketHelper.writeShort((short) 1);
        assertThat(byteReader.isReady()).isTrue();

        bytes = byteReader.read();
        assertThat(bytes).hasSize(2);

        assertThat(byteReader.isReady()).isFalse();

        // third message
        socketHelper.writeShort((short) 0);  //len 1/2
        assertThat(byteReader.isReady()).isFalse();
        socketHelper.writeShort((short) 1); // len 1/2
        assertThat(byteReader.isReady()).isFalse();

        socketHelper.write((byte) 1);

        assertThat(byteReader.isReady()).isTrue();
    }

    @Test
    void multiplePacketsTest() throws IOException {
        socketHelper.writeInt(4); // len
        assertThat(byteReader.isReady()).isFalse();

        socketHelper.writeInt(0);
        assertThat(byteReader.isReady()).isTrue();

        socketHelper.writeInt(4); // len
        assertThat(byteReader.isReady()).isTrue();

        socketHelper.writeInt(0);
        assertThat(byteReader.isReady()).isTrue();

        byte[] bytes = byteReader.read();
        assertThat(byteReader.isReady()).isTrue();

        socketHelper.writeInt(4); // len
        assertThat(byteReader.isReady()).isTrue();

        socketHelper.writeInt(0);
        assertThat(byteReader.isReady()).isTrue();

        bytes = byteReader.read();
        assertThat(byteReader.isReady()).isTrue();

        bytes = byteReader.read();
        assertThat(byteReader.isReady()).isFalse();

    }

}
