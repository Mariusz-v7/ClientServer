package pl.mrugames.nucleus.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class SocketHelperSpec {
    private SocketHelper socketHelper;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void communicationTest() throws IOException {
        ByteBuffer read = socketHelper.read();
        assertThat(read.hasRemaining()).isFalse();

        socketHelper.write((byte) 1, (byte) 2, (byte) 3);
        read = socketHelper.read();
        assertThat(read.hasRemaining()).isTrue();

        assertThat(read.get()).isEqualTo((byte) 1);
        assertThat(read.get()).isEqualTo((byte) 2);
        assertThat(read.get()).isEqualTo((byte) 3);

        socketHelper.write((byte) 3, (byte) 4);
        read = socketHelper.read();

        assertThat(read.get()).isEqualTo((byte) 3);

        socketHelper.write((byte) 5);
        read = socketHelper.read();

        assertThat(read.get()).isEqualTo((byte) 4);
        assertThat(read.get()).isEqualTo((byte) 5);
    }
}
