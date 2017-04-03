package pl.mrugames.commons.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.InOrder;
import pl.mrugames.commons.client.initializers.Initializer;
import pl.mrugames.commons.client.io.ClientReader;
import pl.mrugames.commons.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(BlockJUnit4ClassRunner.class)
public class ClientFactorySpec {
    private ClientFactory clientFactory;
    private Function<OutputStream, ClientWriter> clientWriterProvider;
    private Function<InputStream, ClientReader> clientReaderProvider;
    private ClientWorkerFactory clientWorkerFactory;
    private Socket socket;

    @Before
    public void before() {
        clientWriterProvider = mock(Function.class);
        clientReaderProvider = mock(Function.class);
        clientWorkerFactory = mock(ClientWorkerFactory.class);

        clientFactory = spy(new ClientFactory("test", 0, clientWriterProvider, clientReaderProvider, clientWorkerFactory, Collections.emptyList()));

        socket = mock(Socket.class);
    }

    @Test
    public void givenEmptyListOfInitializers_whenInit_thenReturnResolvedFutureWithNull() throws IOException, ExecutionException, InterruptedException {
        CompletableFuture<Void> future = clientFactory.initialize(socket);
        assertThat(future.get()).isNull();
    }

    @Test
    public void givenListOfInitializers_whenInit_thenInitializersAreCalledOneAfterTheAnother() throws IOException, ExecutionException, InterruptedException {
        List<BiFunction<?, ?, Initializer>> initializers = new LinkedList<>();

        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);
        Initializer initializer3 = mock(Initializer.class);

        initializers.add((a, b) -> initializer1);
        initializers.add((a, b) -> initializer2);
        initializers.add((a, b) -> initializer3);

        InOrder inOrder = inOrder(initializer1, initializer2, initializer3);

        clientFactory = spy(new ClientFactory("test", 0, clientWriterProvider, clientReaderProvider, clientWorkerFactory, initializers));
        clientFactory.initialize(socket).get();

        inOrder.verify(initializer1).run();
        inOrder.verify(initializer2).run();
        inOrder.verify(initializer3).run();
    }


}
