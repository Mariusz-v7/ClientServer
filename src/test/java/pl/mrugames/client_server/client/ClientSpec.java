package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSpec {
    private Client client;
    private List<Initializer> initializers;
    private ClientWorker clientWorker;
    private SocketChannel socket;
    private Comm comm;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        initializers = new LinkedList<>();
        clientWorker = mock(ClientWorker.class);
        socket = mock(SocketChannel.class);
        comm = mock(Comm.class);

        client = spy(new Client("Test Client", initializers, comm, clientWorker, socket, mock(Timer.class)));

        doNothing().when(client).closeChannel(any());
    }

    @Test
    void givenSomeInitializers_whenRun_thenRunAllInitializersInOrderThenCloseSocket() throws IOException {
        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);

        initializers.add(initializer1);
        initializers.add(initializer2);

        client.run();

        InOrder inOrder = Mockito.inOrder(initializer1, initializer2, client);

        inOrder.verify(initializer1).run();
        inOrder.verify(initializer2).run();

        inOrder.verify(client).closeChannel(socket);
    }

    @Test
    void givenInitializerThrowsException_whenFinish_thenCloseSocketAnyway() throws IOException {
        Initializer initializer1 = mock(Initializer.class);

        initializers.add(initializer1);

        doThrow(RuntimeException.class).when(initializer1).run();

        client.run();

        verify(client).closeChannel(socket);
    }

    @Test
    void givenClientNotRun_whenAwaitStart_thenReturnFalse() throws InterruptedException {
        boolean result = assertTimeout(Duration.ofMillis(300), () -> client.awaitStart(250, TimeUnit.MILLISECONDS));

        assertFalse(result);
    }

    @Test
    void givenClientRun_whenAwaitStart_thenReturnTrue() {
        client.run();

        boolean result = assertTimeout(Duration.ofMillis(50), () -> client.awaitStart(250, TimeUnit.MILLISECONDS));

        assertTrue(result);
    }
}
