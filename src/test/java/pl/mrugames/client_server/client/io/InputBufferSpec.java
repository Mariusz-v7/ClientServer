package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import pl.mrugames.client_server.SocketHelper;

import java.io.IOException;

class InputBufferSpec {
    private InputBuffer inputBuffer;

    private SocketHelper socketHelper;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        inputBuffer = new InputBuffer(socketHelper.getInputStream());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

}
