package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.client.filters.FilterProcessorV2;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientFactoryV2Spec {
    private ClientFactoryV2<String, String, String, String> clientFactory;
    private ClientWorkerFactoryV2<String, String, String, String> clientWorkerFactory;
    private List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessorV2 inputFilterProcessor;
    private FilterProcessorV2 outputFilterProcessor;
    private ExecutorService executorService;
    private ClientV2 client;
    private boolean shouldTimeoutClientCreation;
    private ClientWatchdog clientWatchdog;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws InterruptedException {
        initializerFactories = new LinkedList<>();

        inputFilterProcessor = mock(FilterProcessorV2.class);
        outputFilterProcessor = mock(FilterProcessorV2.class);

        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);

        Function<OutputStream, ClientWriter<String>> clientWriterFactory = i -> clientWriter;
        Function<InputStream, ClientReader<String>> clientReaderFactory = o -> clientReader;

        clientWorkerFactory = mock(ClientWorkerFactoryV2.class);

        executorService = mock(ExecutorService.class);

        clientWatchdog = mock(ClientWatchdog.class);

        clientFactory = spy(new ClientFactoryV2<>("factory",
                "client",
                clientWorkerFactory,
                initializerFactories,
                clientWriterFactory,
                clientReaderFactory,
                inputFilterProcessor,
                outputFilterProcessor,
                executorService,
                clientWatchdog));

        doAnswer(a -> {
            client = (ClientV2) a.callRealMethod();
            client = spy(client);
            doReturn(!shouldTimeoutClientCreation).when(client).awaitStart(anyLong(), any());

            return client;
        }).when(clientFactory).createClient(anyString(), anyList(), any(), any());
    }

    @Test
    void givenClientCreated_setProperName() throws Exception {
        ClientV2 client1 = clientFactory.create(mock(Socket.class));
        ClientV2 client2 = clientFactory.create(mock(Socket.class));

        assertThat(client1.getName()).isEqualTo("client-1");
        assertThat(client2.getName()).isEqualTo("client-2");
    }

    @Test
    void givenClientCreated_setProperInitializers() throws Exception {
        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);

        initializerFactories.add((i, o) -> initializer1);
        initializerFactories.add((i, o) -> initializer2);

        ClientV2 client = clientFactory.create(mock(Socket.class));

        assertThat(client.getInitializers()).containsExactly(initializer1, initializer2);
    }

    @Test
    void givenClientCreated_setProperClientWorker() throws Exception {
        Runnable worker = mock(Runnable.class);
        doReturn(worker).when(clientWorkerFactory).create(any(), any());

        ClientV2 client = clientFactory.create(mock(Socket.class));

        assertThat(client.getClientWorker()).isSameAs(worker);
    }

    @Test
    void givenFactoryThrowsException_whenCreate_thenException() throws IOException {
        doThrow(RuntimeException.class).when(clientWorkerFactory).create(any(), any());
        Socket socket = mock(Socket.class);

        assertThrows(RuntimeException.class, () -> clientFactory.create(socket));

        verify(socket).close();
    }

    @Test
    void whenCreateComm_thenSetProperComponents() throws IOException {
        CommV2<String, String, String, String> comm = clientFactory.createComms("test", mock(Socket.class));

        assertThat(comm.getClientReader()).isSameAs(clientReader);
        assertThat(comm.getClientWriter()).isSameAs(clientWriter);
        assertThat(comm.getInputFilterProcessor()).isSameAs(inputFilterProcessor);
        assertThat(comm.getOutputFilterProcessor()).isSameAs(outputFilterProcessor);
    }

    @Test
    void whenCreateClient_thenSubmitToExecutor_andWait() throws Exception {
        clientFactory.create(mock(Socket.class));

        InOrder inOrder = inOrder(client, executorService);

        inOrder.verify(executorService).execute(client);
        inOrder.verify(client).awaitStart(clientFactory.clientStartTimeoutMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Test
    void givenAwaitStartReturnsFalse_thenException_andCloseSocket() throws Exception {
        shouldTimeoutClientCreation = true;

        Socket socket = mock(Socket.class);

        TimeoutException timeout = assertThrows(TimeoutException.class, () -> clientFactory.create(socket));
        assertThat(timeout.getMessage()).isEqualTo("Failed to start client");

        verify(socket).close();
    }

    @Test
    void whenCreateClient_thenRegisterToWatchdog() throws Exception {
        CommV2 comm = mock(CommV2.class);
        doReturn(comm).when(clientFactory).createComms(anyString(), any());

        Socket socket = mock(Socket.class);

        clientFactory.create(socket);

        verify(clientWatchdog).register(comm, socket, client.getName());
    }
}
