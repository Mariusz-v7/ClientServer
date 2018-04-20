package pl.mrugames.nucleus.server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.mrugames.nucleus.server.client.Client;
import pl.mrugames.nucleus.server.client.ClientWorker;
import pl.mrugames.nucleus.server.client.Comm;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientRequestTaskSpec {
    private ClientRequestTask task;
    private Client client;
    private Comm comm;
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
        task.executeTask();
        verify(comm, never()).receive();
    }

    @Test
    void givenCommReceiveReturnsFalse_whenCall_thenReturnImmediately() throws Exception {
        doReturn(null).when(comm).receive();
        task.executeTask();
        verify(worker, never()).onRequest(any());
    }

    @Test
    void givenCanReadThrowsException_whenExecuteTask_thenRethrowIt() throws Exception {
        doThrow(RuntimeException.class).when(comm).canRead();
        assertThrows(RuntimeException.class, task::executeTask);
    }

    @Test
    void givenResponseIsNull_whenCall_thenDoNotSendIt() throws Exception {
        doReturn(null).when(worker).onRequest(any());
        task.executeTask();
        verify(comm, never()).send(any());
    }

    @Test
    void givenMultipleFramesInABuffer_whenCall_thenDistributeTasksInTheSameOrder() throws Exception {
        doReturn(true, true, true, true, true, true, false).when(comm).canRead();
        doReturn("1", null, "2", null, "3", null).when(comm).receive();

        RequestExecuteTask lastTask = task.executeTask();

        ArgumentCaptor<RequestExecuteTask> argumentCaptor = ArgumentCaptor.forClass(RequestExecuteTask.class);
        verify(taskExecutor, times(2)).submit(argumentCaptor.capture(), anyLong());

        List<RequestExecuteTask> allValues = argumentCaptor.getAllValues();

        assertThat(allValues).hasSize(2);

        assertThat(allValues.get(0).getFrame()).isEqualTo("1");
        assertThat(allValues.get(1).getFrame()).isEqualTo("2");

        assertThat(lastTask.getFrame()).isEqualTo("3");
    }

    @Test
    void whenExecuteLastTask_thenJustExecuteIt() throws Exception {
        RequestExecuteTask requestExecuteTask = mock(RequestExecuteTask.class);
        task.executeLastTask(requestExecuteTask);

        verify(requestExecuteTask).call();
    }
}
