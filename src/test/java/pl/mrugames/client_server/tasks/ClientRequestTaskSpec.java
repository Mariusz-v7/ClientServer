package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientRequestTaskSpec {
    private ClientRequestTask task;
    private Client client;
    private Comm<Object, Object, Serializable, Serializable> comm;
    private ClientWorker<Object, Object> worker;
    private TaskExecutor taskExecutor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws Exception {
        client = mock(Client.class);

        comm = mock(Comm.class);
        doReturn(true).when(comm).canRead();
        doReturn("request").when(comm).receive();

        doReturn(comm).when(client).getComm();

        worker = mock(ClientWorker.class);
        doReturn(worker).when(client).getClientWorker();

        taskExecutor = mock(TaskExecutor.class);
        doReturn(taskExecutor).when(client).getTaskExecutor();

        task = new ClientRequestTask(client);

        doReturn("anything").when(worker).onRequest(any());
    }

    @Test
    void givenObjectToReceive_whenCall_thenReadFromComm_andPassToWorker() throws Exception {
        doReturn("abc").when(comm).receive();
        task.call();
        verify(worker).onRequest("abc");
    }

    @Test
    void givenWorkerReturnsObject_whenCall_thenSendViaComm() throws Exception {
        doReturn("def").when(worker).onRequest(any());
        task.call();
        verify(comm).send("def");
    }

    @Test
    void givenCommCanReadReturnsFalse_whenCall_thenReturnImmediately() throws Exception {
        doReturn(false).when(comm).canRead();
        task.call();
        verify(comm, never()).receive();
    }

    @Test
    void givenCommReceiveReturnsFalse_whenCall_thenReturnImmediately() throws Exception {
        doReturn(null).when(comm).receive();
        task.call();
        verify(worker, never()).onRequest(any());
    }

    @Test
    void givenCanReadThrowsException_whenCall_thenSubmitShutdown() throws Exception {
        doThrow(RuntimeException.class).when(comm).canRead();
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenCommThrowsException_whenCall_thenSubmitShutdown() throws Exception {
        doThrow(RuntimeException.class).when(comm).receive();
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenWorkerThrowsException_whenCall_thenSubmitShutdown() throws Exception {
        doThrow(RuntimeException.class).when(worker).onRequest(any());
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenCommSendThrowsException_whenCall_thenSubmitShutdown() throws Exception {
        doThrow(RuntimeException.class).when(comm).send(any());
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenResponseIsNull_whenCall_thenDoNotSendIt() throws Exception {
        doReturn(null).when(worker).onRequest(any());
        task.call();
        verify(comm, never()).send(any());
    }

}
