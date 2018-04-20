package pl.mrugames.nucleus.common.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.nucleus.server.SocketHelper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ByteWriterSpec {
    private SocketHelper socketHelper;
    private ByteWriter byteWriter;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        byteWriter = new ByteWriter(socketHelper.getWriteBuffer());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void writeReadTest() throws IOException {
        byteWriter.write((byte) 1, (byte) 2, (byte) 3);
        byteWriter.write((byte) 3, (byte) 4, (byte) 5, (byte) 6);

        socketHelper.flush();

        int len = socketHelper.getReadBuffer().getInt();
        assertThat(len).isEqualTo(3);

        byte[] bytes = new byte[3];
        socketHelper.getReadBuffer().get(bytes);
        assertThat(bytes).containsExactly(1, 2, 3);

        len = socketHelper.getReadBuffer().getInt();
        assertThat(len).isEqualTo(4);

        bytes = new byte[4];
        socketHelper.getReadBuffer().get(bytes);
        assertThat(bytes).containsExactly(3, 4, 5, 6);
    }

}
