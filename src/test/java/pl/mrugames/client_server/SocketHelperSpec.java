package pl.mrugames.client_server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
        assertThat(socketHelper.getInputStream().available()).isEqualTo(0);

        socketHelper.getOutputStream().write(1);
        socketHelper.getOutputStream().write(1);
        socketHelper.getOutputStream().flush();

        assertThat(socketHelper.getInputStream().available()).isEqualTo(2);
    }
}
