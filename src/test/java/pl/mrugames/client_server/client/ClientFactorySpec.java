package pl.mrugames.client_server.client;

import com.codahale.metrics.MetricRegistry;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.InOrder;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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
    private ClientWorker clientWorker;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void before() {
        HealthCheckManager.setMetricRegistry(new MetricRegistry());
        clientWriterProvider = mock(Function.class);
        clientReaderProvider = mock(Function.class);
        clientWorkerFactory = mock(ClientWorkerFactory.class);

        clientFactory = spy(new ClientFactory("test", 0, clientWriterProvider, clientReaderProvider,
                clientWorkerFactory, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        ));

        socket = mock(Socket.class);

        clientWorker = mock(ClientWorker.class);
        doReturn(clientWorker).when(clientWorkerFactory).create(any(), any(), any());
    }

    @After
    public void after() {
        clientFactory.shutdown();
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

        clientFactory = new ClientFactory("test3", 0, clientWriterProvider, clientReaderProvider, clientWorkerFactory,
                initializers, Collections.emptyList(), Collections.emptyList());
        clientFactory.initialize(socket).get();

        inOrder.verify(initializer1).run();
        inOrder.verify(initializer2).run();
        inOrder.verify(initializer3).run();
    }

    @Test
    public void givenInitializerThrowsException_whenInit_thenExceptionIsNotSwallowed() throws IOException, ExecutionException, InterruptedException {
        Initializer initializer = mock(Initializer.class);
        doThrow(IOExceptionWrapper.class).when(initializer).run();

        List<BiFunction<?, ?, Initializer>> initializers = new LinkedList<>();
        initializers.add((a, b) -> initializer);

        clientFactory = new ClientFactory("test2",
                0, clientWriterProvider, clientReaderProvider,
                clientWorkerFactory, initializers,
                Collections.emptyList(), Collections.emptyList()
        );

        expectedException.expect(ExecutionException.class);
        expectedException.expectCause(IsInstanceOf.instanceOf(IOExceptionWrapper.class));

        clientFactory.initialize(socket).get();
    }

    @Test(timeout = 1000)
    public void givenClientStops_thenClientWorker_onClientTermination_isCalled() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(a -> {
            countDownLatch.countDown();
            return null;
        }).when(clientWorker).onClientTermination();

        clientFactory.initWorker(socket);
        countDownLatch.await();
    }

}