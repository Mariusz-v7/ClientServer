package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TestInitializer implements Initializer {
    private boolean completed;
    private int id;
    private Comm comm;
    String nextResponse;

    public TestInitializer(int id) {
        this.id = id;
        comm = mock(Comm.class);

        try {
            doReturn(true).when(comm).canRead();
            doReturn("initializer" + id + " frame").when(comm).receive();
        } catch (Exception e) {
            e.printStackTrace();
        }

        nextResponse = "response initializer " + id;
    }

    @Override
    public Comm getComm() {
        return comm;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public Object proceed(Object frame) {
        return nextResponse;
    }
}

class ClientRequestTaskSpec {
    private ClientRequestTask task;
    private Client client;
    private Comm<Object, Object, Serializable, Serializable> comm;
    private ClientWorker<Object, Object> worker;
    private TaskExecutor taskExecutor;
    private List<TestInitializer> initializers;

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

        initializers = new LinkedList<>();
        doReturn(initializers).when(client).getInitializers();


        initializers.add(spy(new TestInitializer(1)));
        initializers.add(spy(new TestInitializer(2)));
        initializers.add(spy(new TestInitializer(3)));
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
    void givenExecuteTaskThrowsException_whenCall_thenSubmitShutdownAndRethrow() throws Exception {
        initializers.clear();

        doThrow(RuntimeException.class).when(task).executeTask();
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenRunInitializersThrowsException_whenCall_thenSubmitShutdownAndRethrow() throws Exception {
        doThrow(RuntimeException.class).when(task).runInitializers();
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenResponseIsNull_whenCall_thenDoNotSendIt() throws Exception {
        doReturn(null).when(worker).onRequest(any());
        task.executeTask();
        verify(comm, never()).send(any());
    }

    @Test
    void givenMultipleFramesInABuffer_whenCall_thenDistributeTasksInTheSameOrder() throws Exception {
        doReturn(true, true, true, false).when(comm).canRead();
        doReturn("1", "2", "3").when(comm).receive();

        RequestExecuteTask lastTask = task.executeTask();

        ArgumentCaptor<RequestExecuteTask> argumentCaptor = ArgumentCaptor.forClass(RequestExecuteTask.class);
        verify(taskExecutor, times(2)).submit(argumentCaptor.capture());

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

    @Test
    void givenLastTaskThrowsException_whenCall_thenDontSubmitShutdownTaskButRethrowException() throws Exception {
        doReturn(true).when(task).runInitializers();

        RequestExecuteTask lastTask = mock(RequestExecuteTask.class);

        doReturn(lastTask).when(task).executeTask();

        doThrow(RuntimeException.class).when(task).executeLastTask(lastTask);
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor, never()).submit(any(ClientShutdownTask.class));
    }

    @Test
    void givenInitializerIsNotCompleted_whenCall_thenCallCommAndPassToInitializer() throws Exception {
        task.call();

        verify(initializers.get(0)).proceed("initializer1 frame");
    }

    @Test
    void givenInitializerNotCompletedAndCommNotReady_whenCall_thenDoNotCallProceed() throws Exception {
        Comm comm = initializers.get(0).getComm();
        doReturn(false).when(comm).canRead();

        task.runInitializers();

        for (Initializer initializer : initializers) {
            verify(initializer, never()).proceed(any());
        }
    }

    @Test
    void givenInitializerNotCompletedAndCommReturnsNull_whenCall_thenDoNotSendResponse() throws Exception {
        initializers.get(0).nextResponse = null;

        task.call();

        Comm comm = initializers.get(0).getComm();
        verify(comm, never()).send(any());
    }

    @Test
    void givenInitializerReturnsObject_whenCall_thenSentViaComm() throws Exception {
        task.call();

        Comm comm = initializers.get(0).getComm();

        verify(comm).send("response initializer 1");
    }

    @Test
    void givenInitializerCompletesAfterProceed_whenCall_thenExecuteNextInitializer() throws Exception {
        doAnswer(a -> {
            initializers.get(0).setCompleted(true);

            return a.getArguments()[0];
        }).when(initializers.get(0)).proceed("initializer1 frame");

        task.call();

        verify(initializers.get(1)).proceed("initializer2 frame");
    }

    @Test
    void givenFirstInitializerDoesNotComplete_whenCall_thenDoNotCallNextInitializers() throws Exception {
        task.call();

        verify(initializers.get(1), never()).proceed(any());
        verify(initializers.get(2), never()).proceed(any());
    }

    @Test
    void givenFirstInitializerIsCompleted_whenCall_thenSkipIt() throws Exception {
        initializers.get(0).setCompleted(true);

        task.call();

        verify(initializers.get(0), never()).proceed(any());
        verify(initializers.get(1)).proceed("initializer2 frame");
    }

    @Test
    void givenAllInitializersCompleted_whenCall_thenClearList() throws Exception {
        initializers.get(0).setCompleted(true);
        initializers.get(1).setCompleted(true);
        initializers.get(2).setCompleted(true);

        task.call();

        assertThat(initializers).isEmpty();
    }

    @Test
    void givenInitializerCommReadReturnsNull_whenCall_thenReturn() throws Exception {
        Comm comm = initializers.get(0).getComm();

        doReturn(null).when(comm).receive();

        verify(initializers.get(0), never()).proceed(any());
        verify(initializers.get(1), never()).proceed(any());
        verify(initializers.get(2), never()).proceed(any());
    }

    @Test
    void givenRunInitializersReturnsFalse_whenCall_thenReturn() throws Exception {
        doReturn(false).when(task).runInitializers();
        task.call();
        verify(task, never()).executeTask();
    }
}
