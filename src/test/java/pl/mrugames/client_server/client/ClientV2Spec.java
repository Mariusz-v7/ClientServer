package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

class ClientV2Spec {
    private ClientV2 client;
    private SocketChannel socketChannel;
    private List<Initializer> initializers;

    @BeforeEach
    void before() {
        socketChannel = mock(SocketChannel.class);
        initializers = new LinkedList<>();

        client = spy(new ClientV2("Test Client", socketChannel, initializers));
    }

    @Test
    void givenSomeInitializers_whenInit_thenRunAllInitializersInOrder() throws IOException {
        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);

        initializers.add(initializer1);
        initializers.add(initializer2);

        client.init();

        InOrder inOrder = Mockito.inOrder(initializer1, initializer2);

        inOrder.verify(initializer1).run();
        inOrder.verify(initializer2).run();
    }

    @Test
    void givenInitializerThrowsException_whenInit_thenCloseSocket() throws IOException {
        Initializer initializer1 = mock(Initializer.class);

        initializers.add(initializer1);

        doThrow(RuntimeException.class).when(initializer1).run();

        client.init();

        verify(client).close();
    }


}
