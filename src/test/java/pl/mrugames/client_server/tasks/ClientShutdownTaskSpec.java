package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClientShutdownTaskSpec {
    private ClientShutdownTask task;
    private Client client;
    private ClientWorker clientWorker;
    private Comm comm;
    private AtomicBoolean shutdown;

    @BeforeEach
    void before() {
        client = mock(Client.class);

        clientWorker = mock(ClientWorker.class);
        doReturn(clientWorker).when(client).getClientWorker();

        comm = mock(Comm.class);
        doReturn(comm).when(client).getComm();

        shutdown = new AtomicBoolean();
        doReturn(shutdown).when(client).getShutdown();

        task = spy(new ClientShutdownTask(client));
        doNothing().when(task).closeChannel();
    }

    @Test
    void whenCall_thenInvokeClientWorkerOnShutdown() throws Exception {
        task.call();
        verify(clientWorker).onShutdown();
    }

    @Test
    void whenCall_thenShutdownChannel() throws Exception {
        task.call();
        verify(task).closeChannel();
    }

    @Test
    void givenOnShutdownThrowsException_whenCall_thenCallCloseChannelInFinallyBlock() {
        doThrow(RuntimeException.class).when(clientWorker).onShutdown();
        assertThrows(RuntimeException.class, task::call);
        verify(task).closeChannel();
    }

    @Test
    void givenOnShutdownReturnsNull_whenCall_thenDoNotSendItViaComm() throws Exception {
        doReturn(null).when(clientWorker).onShutdown();
        task.call();
        verify(comm, never()).send(any());
    }

    @Test
    void givenOnShutdownReturnsObject_whenCall_thenSendIt() throws Exception {
        doReturn("task").when(clientWorker).onShutdown();
        task.call();
        verify(comm).send("task");
    }

    @Test
    void whenCall_thenSetShutdown() throws Exception {
        task.call();
        assertThat(shutdown.get()).isTrue();
    }

    @Test
    void givenClientIsShutdown_whenCall_thenException() {
        shutdown.set(true);
        IllegalStateException e = assertThrows(IllegalStateException.class, task::call);
        assertThat(e.getMessage()).isEqualTo("Client was already shutdown.");
    }
}
