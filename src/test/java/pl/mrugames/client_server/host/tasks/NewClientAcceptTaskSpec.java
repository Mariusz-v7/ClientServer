package pl.mrugames.client_server.host.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NewClientAcceptTaskSpec {
    private NewClientAcceptTask task;
    private ClientFactory clientFactory;
    private Selector selector;
    private SocketChannel clientChannel;
    private Client client;
    private ExecutorService clientExecutor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void before() throws Exception {
        clientFactory = mock(ClientFactory.class);
        selector = mock(Selector.class);
        clientChannel = mock(SocketChannel.class);
        client = mock(Client.class);

        clientExecutor = mock(ExecutorService.class);

        doReturn(client).when(clientFactory).create(clientChannel, clientExecutor);

        task = spy(new NewClientAcceptTask("Test host", clientFactory, clientChannel, selector, clientExecutor));
        doNothing().when(task).configure(clientChannel);
        doNothing().when(task).close(clientChannel);
        doNothing().when(task).register(clientChannel, client);
    }

    @Test
    void whenAccept_thenConfigure_andCallFactory_andRegisterSelector_andReturnClient() throws Exception {
        InOrder inOrder = inOrder(task, clientFactory);

        Client result = task.call();

        inOrder.verify(task).configure(clientChannel);
        inOrder.verify(clientFactory).create(clientChannel, clientExecutor);
        inOrder.verify(task).register(clientChannel, client);

        assertThat(result).isSameAs(client);
    }

    @Test
    void givenConfigureThrowsException_whenCall_thenCloseSocketAndRethrow() throws IOException {
        RuntimeException e = new RuntimeException();
        doThrow(e).when(task).configure(clientChannel);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> task.call());

        assertThat(thrown).isSameAs(e);
        verify(task).close(clientChannel);
    }

    @Test
    void givenFactoryThrowsException_whenCall_thenCloseSocketAndRethrow() throws Exception {
        RuntimeException e = new RuntimeException();
        doThrow(e).when(clientFactory).create(clientChannel, clientExecutor);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> task.call());

        assertThat(thrown).isSameAs(e);
        verify(task).close(clientChannel);
    }

    @Test
    void givenRegisterThrowsException_whenCall_thenCloseSocketAndRethrow() throws Exception {
        RuntimeException e = new RuntimeException();
        doThrow(e).when(task).register(clientChannel, client);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> task.call());

        assertThat(thrown).isSameAs(e);
        verify(task).close(clientChannel);
    }


}
