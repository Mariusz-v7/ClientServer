package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.client.*;

import java.nio.channels.SocketChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NewClientAcceptTaskSpec {
    private NewClientAcceptTask task;
    private ClientFactory clientFactory;
    private SocketChannel clientChannel;
    private Client client;
    private TaskExecutor clientExecutor;
    private ClientWorker clientWorker;
    private Comm comm;
    private ClientWatchdog watchdog;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void before() throws Exception {
        clientFactory = mock(ClientFactory.class);
        client = mock(Client.class);
        clientWorker = mock(ClientWorker.class);
        comm = mock(Comm.class);
        clientChannel = mock(SocketChannel.class);
        watchdog = mock(ClientWatchdog.class);

        doReturn(clientWorker).when(client).getClientWorker();
        doReturn(comm).when(client).getComm();

        clientExecutor = mock(TaskExecutor.class);
        doReturn(clientExecutor).when(client).getTaskExecutor();

        doReturn(client).when(clientFactory).create(clientChannel, clientExecutor, watchdog);

        task = spy(new NewClientAcceptTask("Test host", clientFactory, clientChannel, clientExecutor, watchdog));
        doNothing().when(task).close(clientChannel);
    }

    @Test
    void whenAccept_thenCallFactory_andReturnClient() throws Exception {
        InOrder inOrder = inOrder(task, clientFactory);

        Client result = task.call();

        inOrder.verify(clientFactory).create(clientChannel, clientExecutor, watchdog);

        assertThat(result).isSameAs(client);
    }

    @Test
    void givenFactoryThrowsException_whenCall_thenCloseSocketAndRethrow() throws Exception {
        RuntimeException e = new RuntimeException();
        doThrow(e).when(clientFactory).create(clientChannel, clientExecutor, watchdog);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> task.call());

        assertThat(thrown).isSameAs(e);
        verify(task).close(clientChannel);
    }

    @Test
    void whenAcceptNewClient_thenCallOnInit() throws Exception {
        task.call();
        verify(clientWorker).onInit();
    }

    @Test
    void givenOnInitReturnsString_whenCall_thenSendIt() throws Exception {
        doReturn("hello").when(clientWorker).onInit();
        task.call();
        verify(comm).send("hello");
    }

    @Test
    void givenOnInitReturnsNull_whenCall_thenDoNotSendIt() throws Exception {
        doReturn(null).when(clientWorker).onInit();
        task.call();
        verify(comm, never()).send(any());
    }

    @Test
    void givenOnInitThrowsException_whenCall_thenSubmitShutdownTaskWithoutClosingTheChannel() throws Exception {
        doThrow(RuntimeException.class).when(clientWorker).onInit();
        assertThrows(RuntimeException.class, task::call);
        verify(clientExecutor).submit(any(ClientShutdownTask.class));
        verify(task, never()).close(any());
    }
}
