package pl.mrugames.client_server.host.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NewClientAcceptTaskSpec {
    private NewClientAcceptTask task;
    private ClientFactory clientFactory;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private SocketChannel clientChannel;
    private Client client;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void before() throws Exception {
        clientFactory = mock(ClientFactory.class);
        selector = mock(Selector.class);
        serverSocketChannel = mock(ServerSocketChannel.class);
        clientChannel = mock(SocketChannel.class);
        client = mock(Client.class);

        doReturn(clientChannel).when(serverSocketChannel).accept();
        doReturn(client).when(clientFactory).create(clientChannel);

        task = spy(new NewClientAcceptTask("Test host", clientFactory, serverSocketChannel, selector));
        doNothing().when(task).configure(clientChannel);
        doNothing().when(task).close(clientChannel);
        doNothing().when(task).register(clientChannel, client);
    }

    @Test
    void givenAcceptThrowsException_whenCall_thenRethrowIt() throws IOException {
        RuntimeException e = new RuntimeException();
        doThrow(e).when(serverSocketChannel).accept();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> task.call());

        assertThat(thrown).isSameAs(e);
    }

    @Test
    void whenAccept_thenConfigure_andCallFactory_andRegisterSelector_andReturnClient() throws Exception {
        InOrder inOrder = inOrder(task, clientFactory);

        Client result = task.call();

        inOrder.verify(task).configure(clientChannel);
        inOrder.verify(clientFactory).create(clientChannel);
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
        doThrow(e).when(clientFactory).create(clientChannel);

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
