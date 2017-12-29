package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClientShutdownTaskSpec {
    private ClientShutdownTask task;
    private Client client;
    private ClientWorker clientWorker;
    private Comm comm;

    @BeforeEach
    void before() {
        client = mock(Client.class);

        clientWorker = mock(ClientWorker.class);
        doReturn(clientWorker).when(client).getClientWorker();

        comm = mock(Comm.class);
        doReturn(comm).when(client).getComm();

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
}
