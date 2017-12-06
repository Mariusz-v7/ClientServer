package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientFactoryV2Spec {
    private ClientFactoryV2<String, String, String, String> clientFactory;
    private ClientWorkerFactoryV2<String, String, String, String> clientWorkerFactory;
    private List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        initializerFactories = new LinkedList<>();

        clientWorkerFactory = mock(ClientWorkerFactoryV2.class);
        clientFactory = new ClientFactoryV2<>("factory", "client", clientWorkerFactory, initializerFactories);
    }

    @Test
    void givenClientCreated_setProperName() throws Exception {
        ClientV2 client1 = clientFactory.create(mock(Socket.class));
        ClientV2 client2 = clientFactory.create(mock(Socket.class));

        assertThat(client1.getName()).isEqualTo("client-1");
        assertThat(client2.getName()).isEqualTo("client-2");
    }

    @Test
    void givenClientCreated_setProperInitializers() throws Exception {
        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);

        initializerFactories.add((i, o) -> initializer1);
        initializerFactories.add((i, o) -> initializer2);

        ClientV2 client = clientFactory.create(mock(Socket.class));

        assertThat(client.getInitializers()).containsExactly(initializer1, initializer2);
    }

    @Test
    void givenClientCreated_setProperClientWorker() throws Exception {
        Runnable worker = mock(Runnable.class);
        doReturn(worker).when(clientWorkerFactory).create(any(), any());

        ClientV2 client = clientFactory.create(mock(Socket.class));

        assertThat(client.getClientWorker()).isSameAs(worker);
    }

    @Test
    void givenFactoryThrowsException_whenCreate_thenException() throws IOException {
        doThrow(RuntimeException.class).when(clientWorkerFactory).create(any(), any());
        Socket socket = mock(Socket.class);

        assertThrows(RuntimeException.class, () -> clientFactory.create(socket));

        verify(socket).close();
    }
}
