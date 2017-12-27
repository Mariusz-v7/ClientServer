package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.initializers.Initializer;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientFactorySpec {
    private ClientFactory<String, String, String, String> clientFactory;
    private ClientWorkerFactory<String, String, String, String> clientWorkerFactory;
    private List<BiFunction<InputStream, OutputStream, Initializer>> initializerFactories;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessor inputFilterProcessor;
    private FilterProcessor outputFilterProcessor;
    private ExecutorService executorService;
    private Client client;
    private boolean shouldTimeoutClientCreation;
    private ClientWatchdog clientWatchdog;
    private SocketChannel mockSocketChannel;
    private Socket mockSocket;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws InterruptedException {
        mockSocketChannel = mock(SocketChannel.class);
        mockSocket = mock(Socket.class);

        doReturn(mockSocket).when(mockSocketChannel).socket();

        initializerFactories = new LinkedList<>();

        inputFilterProcessor = mock(FilterProcessor.class);
        outputFilterProcessor = mock(FilterProcessor.class);

        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);

        Function<ByteBuffer, ClientWriter<String>> clientWriterFactory = i -> clientWriter;
        Function<ByteBuffer, ClientReader<String>> clientReaderFactory = o -> clientReader;

        clientWorkerFactory = mock(ClientWorkerFactory.class);

        executorService = mock(ExecutorService.class);

        clientWatchdog = mock(ClientWatchdog.class);
        doReturn(true).when(clientWatchdog).isRunning();

        clientFactory = spy(new ClientFactory<>("factory",
                "client",
                clientWorkerFactory,
                initializerFactories,
                clientWriterFactory,
                clientReaderFactory,
                inputFilterProcessor,
                outputFilterProcessor,
                clientWatchdog,
                1024));

        doNothing().when(clientFactory).closeChannel(any());

        doAnswer(a -> {
            client = (Client) a.callRealMethod();
            client = spy(client);

            return client;
        }).when(clientFactory).createClient(anyString(), any(), anyList(), any(), any(), any());
    }

    @Test
    void givenClientCreated_setProperName() throws Exception {
        Client client1 = clientFactory.create(mockSocketChannel, executorService);
        Client client2 = clientFactory.create(mockSocketChannel, executorService);

        assertThat(client1.getName()).isEqualTo("client-1");
        assertThat(client2.getName()).isEqualTo("client-2");
    }

    @Test
    void givenClientCreated_setProperInitializers() throws Exception {
        Initializer initializer1 = mock(Initializer.class);
        Initializer initializer2 = mock(Initializer.class);

        initializerFactories.add((i, o) -> initializer1);
        initializerFactories.add((i, o) -> initializer2);

        Client client = clientFactory.create(mockSocketChannel, executorService);

        assertThat(client.getInitializers()).containsExactly(initializer1, initializer2);
    }

    @Test
    void givenClientCreated_setProperClientWorker() throws Exception {
        ClientWorker worker = mock(ClientWorker.class);
        doReturn(worker).when(clientWorkerFactory).create(any(), any());

        Client client = clientFactory.create(mockSocketChannel, executorService);

        assertThat(client.getClientWorker()).isSameAs(worker);
    }

    @Test
    void givenFactoryThrowsException_whenCreate_thenException() throws IOException {
        doThrow(RuntimeException.class).when(clientWorkerFactory).create(any(), any());

        assertThrows(RuntimeException.class, () -> clientFactory.create(mockSocketChannel, executorService));

        verify(clientFactory).closeChannel(mockSocketChannel);
    }

    @Test
    void whenCreateComm_thenSetProperComponents() throws IOException {
        Comm<String, String, String, String> comm = clientFactory.createComms("test", mock(SocketChannel.class));

        assertThat(comm.getClientReader()).isSameAs(clientReader);
        assertThat(comm.getClientWriter()).isSameAs(clientWriter);
        assertThat(comm.getInputFilterProcessor()).isSameAs(inputFilterProcessor);
        assertThat(comm.getOutputFilterProcessor()).isSameAs(outputFilterProcessor);
    }

    @Test
    void whenCreateClient_thenRegisterToWatchdog() throws Exception {
        Comm comm = mock(Comm.class);
        doReturn(comm).when(clientFactory).createComms(anyString(), any());

        clientFactory.create(mockSocketChannel, executorService);

        verify(clientWatchdog).register(comm, mockSocketChannel, client.getName());
    }

    @Test
    void givenWatchdogNotRunning_whenCreate_thenException() {
        doReturn(false).when(clientWatchdog).isRunning();

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> clientFactory.create(mock(SocketChannel.class), executorService));
        assertThat(illegalStateException.getMessage()).isEqualTo("Client Watchdog is dead! Cannot accept new connection.");
    }

}
