package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.SocketHelper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ByteWriterSpec {
    private SocketHelper socketHelper;
    private ByteReader byteReader;
    private ByteWriter byteWriter;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        byteReader = new ByteReader(socketHelper.getInputStream());
        byteWriter = new ByteWriter(socketHelper.getOutputStream());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
        byteReader.close();
        byteWriter.close();
    }

    @Test
    void writeReadTest() throws IOException {
        byteWriter.write((byte) 1, (byte) 2, (byte) 3);
        byteWriter.write((byte) 3, (byte) 4, (byte) 5, (byte) 6);

        byte[] bytes = byteReader.read();
        assertThat(bytes).containsExactly(1, 2, 3);

        bytes = byteReader.read();
        assertThat(bytes).containsExactly(3, 4, 5, 6);
    }

}
