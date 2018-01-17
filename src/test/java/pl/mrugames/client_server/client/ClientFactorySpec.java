package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientFactorySpec {
    private ClientFactory<String, String, String, String> clientFactory;
    private ClientWorkerFactory<String, String, String, String> clientWorkerFactory;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessor inputFilterProcessor;
    private FilterProcessor outputFilterProcessor;
    private TaskExecutor executorService;
    private Client client;
    private boolean shouldTimeoutClientCreation;
    private ClientWatchdog clientWatchdog;
    private SocketChannel mockSocketChannel;
    private Socket mockSocket;
    private Function<ByteBuffer, ClientReader<String>> clientReaderFactory;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws InterruptedException {
        mockSocketChannel = mock(SocketChannel.class);
        mockSocket = mock(Socket.class);

        doReturn(mockSocket).when(mockSocketChannel).socket();

        inputFilterProcessor = mock(FilterProcessor.class);
        outputFilterProcessor = mock(FilterProcessor.class);

        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);

        Function<ByteBuffer, ClientWriter<String>> clientWriterFactory = i -> clientWriter;
        clientReaderFactory = mock(Function.class);
        doReturn(clientReader).when(clientReaderFactory).apply(any());

        clientWorkerFactory = mock(ClientWorkerFactory.class);

        executorService = mock(TaskExecutor.class);

        clientWatchdog = mock(ClientWatchdog.class);
        doReturn(true).when(clientWatchdog).isRunning();

        clientFactory = spy(new ClientFactory<>("factory",
                "client",
                clientWorkerFactory,
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
        }).when(clientFactory).createClient(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void givenClientCreated_setProperName() throws Exception {
        Client client1 = clientFactory.create(mockSocketChannel, executorService);
        Client client2 = clientFactory.create(mockSocketChannel, executorService);

        assertThat(client1.getName()).isEqualTo("client-1");
        assertThat(client2.getName()).isEqualTo("client-2");
    }

    @Test
    void givenClientCreated_setProperClientWorker() throws Exception {
        ClientWorker worker = mock(ClientWorker.class);
        doReturn(worker).when(clientWorkerFactory).create(any(), any(), any());

        Client client = clientFactory.create(mockSocketChannel, executorService);

        assertThat(client.getClientWorker()).isSameAs(worker);
    }

    @Test
    void givenFactoryThrowsException_whenCreate_thenException() throws IOException {
        doThrow(RuntimeException.class).when(clientWorkerFactory).create(any(), any(), any());

        assertThrows(RuntimeException.class, () -> clientFactory.create(mockSocketChannel, executorService));

        verify(clientFactory).closeChannel(mockSocketChannel);
    }

    @Test
    void whenCreateComm_thenSetProperComponents() throws IOException {
        ByteBuffer readBuffer = mock(ByteBuffer.class);
        ByteBuffer writeBuffer = mock(ByteBuffer.class);

        Comm comm = clientFactory.createComms("test", mock(SocketChannel.class), readBuffer, writeBuffer);

        assertThat(comm.getClientReader()).isSameAs(clientReader);
        assertThat(comm.getClientWriter()).isSameAs(clientWriter);
        assertThat(comm.getInputFilterProcessor()).isSameAs(inputFilterProcessor);
        assertThat(comm.getOutputFilterProcessor()).isSameAs(outputFilterProcessor);
        assertThat(comm.getWriteBuffer()).isSameAs(writeBuffer);

        ArgumentCaptor<ByteBuffer> argumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

        verify(clientReaderFactory).apply(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue()).isSameAs(readBuffer);
    }

    @Test
    void whenCreateClient_thenRegisterToWatchdog() throws Exception {
        Client client = clientFactory.create(mockSocketChannel, executorService);

        verify(clientWatchdog).register(client);
    }

    @Test
    void givenWatchdogNotRunning_whenCreate_thenException() {
        doReturn(false).when(clientWatchdog).isRunning();

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> clientFactory.create(mock(SocketChannel.class), executorService));
        assertThat(illegalStateException.getMessage()).isEqualTo("Client Watchdog is dead! Cannot accept new connection.");
    }

    @Test
    void whenCreateClient_thenReadBufferIsInReadMode() throws Exception {

        Client<String, String, String, String> client = clientFactory.create(mockSocketChannel, executorService);


        assertThat(client.getReadBuffer().limit()).isEqualTo(client.getReadBuffer().position());
    }

}
