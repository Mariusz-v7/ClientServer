package pl.mrugames.client_server.host.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.io.Serializable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientRequestTaskSpec {
    private ClientRequestTask task;
    private Comm<Object, Object, Serializable, Serializable> comm;
    private ClientWorker<Object, Object> worker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        comm = mock(Comm.class);
        worker = mock(ClientWorker.class);

        task = new ClientRequestTask("Test", comm, worker);
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
}
