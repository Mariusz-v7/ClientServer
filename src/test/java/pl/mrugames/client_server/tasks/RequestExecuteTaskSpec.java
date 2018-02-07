package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.client.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class RequestExecuteTaskSpec {
    private Comm comm;
    private ClientWorker worker;
    private RequestExecuteTask task;
    private TaskExecutor taskExecutor;
    private Client client;

    @BeforeEach
    void before() {
        comm = mock(Comm.class);
        worker = mock(ClientWorker.class);
        taskExecutor = mock(TaskExecutor.class);

        doReturn("response").when(worker).onRequest("request");

        client = mock(Client.class);
        doReturn(comm).when(client).getComm();
        doReturn(worker).when(client).getClientWorker();
        doReturn(taskExecutor).when(client).getTaskExecutor();

        task = new RequestExecuteTask(client, "request");
    }

    @Test
    void whenCall_thenPassToClientWorker_andSendResponse() throws Exception {
        task.call();
        verify(worker).onRequest("request");

        verify(comm).send("response");
    }

    @Test
    void givenResponseIsNull_whenCall_thenCallClientWorker_butDontSendBack() throws Exception {
        doReturn(null).when(worker).onRequest("request");

        task.call();
        verify(worker).onRequest("request");

        verify(comm, never()).send(any());
    }

    @Test
    void givenWorkerThrowsException_whenCall_thenSubmitShutdown() throws Exception {
        doThrow(RuntimeException.class).when(worker).onRequest(any());
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class), anyLong());
    }

    @Test
    void givenCommSendThrowsException_whenCall_thenSubmitShutdown() throws Exception {
        doThrow(RuntimeException.class).when(comm).send(any());
        assertThrows(RuntimeException.class, task::call);
        verify(taskExecutor).submit(any(ClientShutdownTask.class), anyLong());
    }

    @Test
    void givenSwitchProtocolAfterResponse_whenCall_thenSendResponseSwitchProtocolAndSetClearSwitchProtocolField() throws Exception {
        doReturn(new ProtocolSwitch("test", SwitchProtocolStrategy.AFTER_RESPONSE_SENT)).when(client).getProtocolSwitch();

        task.call();

        InOrder inOrder = inOrder(client, comm, worker);
        inOrder.verify(worker).onRequest("request");
        inOrder.verify(comm).send("response");
        inOrder.verify(comm).switchProtocol("test");
        inOrder.verify(client).scheduleProtocolSwitch(null);
    }

    @Test
    void givenSwitchProtocolBeforeResponse_whenCall_thenSwitchProtocolClearSwitchProtocolFieldAndSendResponse() throws Exception {
        doReturn(new ProtocolSwitch("test", SwitchProtocolStrategy.BEFORE_RESPONSE_SENT)).when(client).getProtocolSwitch();

        task.call();

        InOrder inOrder = inOrder(client, comm, worker);
        inOrder.verify(worker).onRequest("request");
        inOrder.verify(comm).switchProtocol("test");
        inOrder.verify(client).scheduleProtocolSwitch(null);
        inOrder.verify(comm).send("response");
    }

}
