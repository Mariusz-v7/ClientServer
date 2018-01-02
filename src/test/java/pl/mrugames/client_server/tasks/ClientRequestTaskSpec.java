package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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
        doReturn(true, false).when(comm).canRead();
        doReturn("request").when(comm).receive();

        doReturn(comm).when(client).getComm();

        worker = mock(ClientWorker.class);
        doReturn(worker).when(client).getClientWorker();

        taskExecutor = mock(TaskExecutor.class);
        doReturn(taskExecutor).when(client).getTaskExecutor();

        task = spy(new ClientRequestTask(client));

        doReturn("anything").when(worker).onRequest(any());
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
    void givenResponseIsNull_whenCall_thenDoNotSendIt() throws Exception {
        doReturn(null).when(worker).onRequest(any());
        task.call();
        verify(comm, never()).send(any());
    }

    @Test
    void givenMultipleFramesInABuffer_whenCall_thenDistributeTasksInTheSameOrder() throws Exception {
        doReturn(true, true, true, false).when(comm).canRead();
        doReturn("1", "2", "3").when(comm).receive();

        task.call();

        ArgumentCaptor<RequestExecuteTask> argumentCaptor = ArgumentCaptor.forClass(RequestExecuteTask.class);
        verify(taskExecutor, times(2)).submit(argumentCaptor.capture());

        List<RequestExecuteTask> allValues = argumentCaptor.getAllValues();

        assertThat(allValues).hasSize(2);

        assertThat(allValues.get(0).getFrame()).isEqualTo("1");
        assertThat(allValues.get(1).getFrame()).isEqualTo("2");

        // last task in the same thread
        argumentCaptor = ArgumentCaptor.forClass(RequestExecuteTask.class);
        verify(task).executeLastTask(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getFrame()).isEqualTo("3");
    }

    @Test
    void whenExecuteLastTask_thenJustExecuteIt() throws Exception {
        RequestExecuteTask requestExecuteTask = mock(RequestExecuteTask.class);
        task.executeLastTask(requestExecuteTask);

        verify(requestExecuteTask).call();
    }

    @Test
    void givenLastTaskThrowsException_whenCall_thenDontSubmitShutdownTaskButRethrowException() throws Exception {
        doThrow(RuntimeException.class).when(task).executeLastTask(any());
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor, never()).submit(any(ClientShutdownTask.class));
    }

}
