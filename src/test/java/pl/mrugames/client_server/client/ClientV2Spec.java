package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

class ClientV2Spec {
    private ClientV2 client;
    private List<Initializer> initializers;
    private Runnable clientWorker;
    private Runnable shutdownNotifier;

    @BeforeEach
    void before() {
        initializers = new LinkedList<>();
        clientWorker = mock(Runnable.class);
        shutdownNotifier = mock(Runnable.class);

        client = new ClientV2("Test Client", initializers, clientWorker, shutdownNotifier);
    }

    @Test
    void givenSomeInitializers_whenRun_thenRunAllInitializersInOrderThenWorkerThenNotifier() {
        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);

        initializers.add(initializer1);
        initializers.add(initializer2);

        client.run();

        InOrder inOrder = Mockito.inOrder(initializer1, initializer2, clientWorker, shutdownNotifier);

        inOrder.verify(initializer1).run();
        inOrder.verify(initializer2).run();

        inOrder.verify(clientWorker).run();

        inOrder.verify(shutdownNotifier).run();
    }

    @Test
    void givenMainLoopThrowsException_whenFinish_thenCallNotifierAnyway() {
        doThrow(RuntimeException.class).when(clientWorker).run();

        client.run();

        verify(shutdownNotifier).run();
    }

    @Test
    void givenInitializerThrowsException_whenFinish_thenCallNotifierAnyway() {
        Initializer initializer1 = mock(Initializer.class);

        initializers.add(initializer1);

        doThrow(RuntimeException.class).when(initializer1).run();

        client.run();

        verify(shutdownNotifier).run();
    }
}
