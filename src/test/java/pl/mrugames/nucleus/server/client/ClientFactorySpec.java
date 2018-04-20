package pl.mrugames.nucleus.server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.mrugames.nucleus.server.client.filters.FilterProcessor;
import pl.mrugames.nucleus.server.client.io.ClientReader;
import pl.mrugames.nucleus.server.client.io.ClientWriter;
import pl.mrugames.nucleus.server.tasks.TaskExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientFactorySpec {
    private ClientFactory<String, String> clientFactory;
    private ClientWorkerFactory<String, String> clientWorkerFactory;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessor inputFilterProcessor;
    private FilterProcessor outputFilterProcessor;
    private TaskExecutor executorService;
    private Client client;
    private ConnectionWatchdog connectionWatchdog;
    private SocketChannel mockSocketChannel;
    private Socket mockSocket;
    private Function<ByteBuffer, ClientReader<String>> clientReaderFactory;
    Function<ByteBuffer, ClientWriter<String>> clientWriterFactory;

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

        clientWriterFactory = i -> clientWriter;
        clientReaderFactory = mock(Function.class);
        doReturn(clientReader).when(clientReaderFactory).apply(any());

        clientWorkerFactory = mock(ClientWorkerFactory.class);

        executorService = mock(TaskExecutor.class);

        connectionWatchdog = mock(ConnectionWatchdog.class);
        doReturn(true).when(connectionWatchdog).isRunning();

        List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories = new LinkedList<>();
        protocolFactories.add(new ProtocolFactory<>(clientWriterFactory, clientReaderFactory, inputFilterProcessor, outputFilterProcessor, "default"));
        protocolFactories.add(new ProtocolFactory<>(mock(Function.class), mock(Function.class), mock(FilterProcessor.class), mock(FilterProcessor.class), "mock1"));
        protocolFactories.add(new ProtocolFactory<>(mock(Function.class), mock(Function.class), mock(FilterProcessor.class), mock(FilterProcessor.class), "mock2"));

        clientFactory = spy(new ClientFactory<>("factory",
                "client",
                clientWorkerFactory,
                protocolFactories,
                1024,
                30,
                30));

        doNothing().when(clientFactory).closeChannel(any());

        doAnswer(a -> {
            client = (Client) a.callRealMethod();
            client = spy(client);

            return client;
        }).when(clientFactory).createClient(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenClientCreated_setProperName() throws Exception {
        Client client1 = clientFactory.create(mockSocketChannel, executorService, connectionWatchdog);
        Client client2 = clientFactory.create(mockSocketChannel, executorService, connectionWatchdog);

        assertThat(client1.getName()).isEqualTo("client-1");
        assertThat(client2.getName()).isEqualTo("client-2");
    }

    @Test
    void givenClientCreated_setProperClientWorker() throws Exception {
        ClientWorker worker = mock(ClientWorker.class);
        doReturn(worker).when(clientWorkerFactory).create(any(), any(), any());

        Client client = clientFactory.create(mockSocketChannel, executorService, connectionWatchdog);

        assertThat(client.getClientWorker()).isSameAs(worker);
    }

    @Test
    void givenFactoryThrowsException_whenCreate_thenException() throws IOException {
        doThrow(RuntimeException.class).when(clientWorkerFactory).create(any(), any(), any());

        assertThrows(RuntimeException.class, () -> clientFactory.create(mockSocketChannel, executorService, connectionWatchdog));

        verify(clientFactory).closeChannel(mockSocketChannel);
    }

    @Test
    void whenCreateComm_thenSetProperComponents() throws IOException {
        ByteBuffer readBuffer = mock(ByteBuffer.class);
        ByteBuffer writeBuffer = mock(ByteBuffer.class);

        Lock readLock = mock(Lock.class);
        Lock writeLock = mock(Lock.class);

        Comm comm = clientFactory.createComms("test", mock(SocketChannel.class), readBuffer, writeBuffer, readLock, writeLock);

        assertThat(comm.getClientReader()).isSameAs(clientReader);
        assertThat(comm.getClientWriter()).isSameAs(clientWriter);
        assertThat(comm.getInputFilterProcessor()).isSameAs(inputFilterProcessor);
        assertThat(comm.getOutputFilterProcessor()).isSameAs(outputFilterProcessor);
        assertThat(comm.getWriteBuffer()).isSameAs(writeBuffer);
        assertThat(comm.getWriteBufferLock()).isSameAs(writeLock);
        assertThat(comm.getReadBufferLock()).isSameAs(readLock);

        ArgumentCaptor<ByteBuffer> argumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

        verify(clientReaderFactory).apply(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue()).isSameAs(readBuffer);
    }

    @Test
    void whenCreateClient_thenRegisterToWatchdog() throws Exception {
        Client client = clientFactory.create(mockSocketChannel, executorService, connectionWatchdog);

        verify(connectionWatchdog).register(client);
    }

    @Test
    void whenCreateClient_thenReadBufferIsInReadMode() throws Exception {
        Client<String, String> client = clientFactory.create(mockSocketChannel, executorService, connectionWatchdog);

        assertThat(client.getReadBuffer().limit()).isEqualTo(client.getReadBuffer().position());
    }

    @Test
    void protocolKeysShouldBeSameAsProtocolNames() throws Exception {
        Client<String, String> client = clientFactory.create(mockSocketChannel, executorService, connectionWatchdog);
        client.getComm().getProtocols().forEach((key, protocol) ->
                assertThat(key).isEqualTo(protocol.getName())
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenDuplicateProtocolName_whenCreate_thenException() {
        List<ProtocolFactory<? extends Serializable, ? extends Serializable>> protocolFactories = new LinkedList<>();
        protocolFactories.add(new ProtocolFactory<>(clientWriterFactory, clientReaderFactory, inputFilterProcessor, outputFilterProcessor, "default"));
        protocolFactories.add(new ProtocolFactory<>(mock(Function.class), mock(Function.class), mock(FilterProcessor.class), mock(FilterProcessor.class), "mock1"));
        protocolFactories.add(new ProtocolFactory<>(mock(Function.class), mock(Function.class), mock(FilterProcessor.class), mock(FilterProcessor.class), "mock1"));

        clientFactory = new ClientFactory<>("factory",
                "client",
                clientWorkerFactory,
                protocolFactories,
                1024,
                30,
                30);

        ByteBuffer readBuffer = mock(ByteBuffer.class);
        ByteBuffer writeBuffer = mock(ByteBuffer.class);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> clientFactory.createComms("", mockSocketChannel, readBuffer, writeBuffer, mock(Lock.class), mock(Lock.class)));

        assertThat(e.getMessage()).isEqualTo("Duplicate protocol name: 'mock1'");
    }

}
