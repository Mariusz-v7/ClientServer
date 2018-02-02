package pl.mrugames.client_server.host;

import com.codahale.metrics.MetricFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.client.ClientWatchdog;
import pl.mrugames.client_server.tasks.ClientRequestTask;
import pl.mrugames.client_server.tasks.ClientShutdownTask;
import pl.mrugames.client_server.tasks.NewClientAcceptTask;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HostManagerSpec {
    private HostManager hostManager;
    private Host host;
    private ServerSocketChannel serverSocketChannel;
    private SocketChannel socketChannel;
    private TaskExecutor clientExecutor;
    private Future<Client> acceptResult;
    private Client client;
    private ByteBuffer readBuffer;
    private ExecutorService executorService;
    private ExecutorService maintenanceExecutor;
    private ClientWatchdog clientWatchdog;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        executorService = mock(ExecutorService.class);
        clientExecutor = mock(TaskExecutor.class);
        maintenanceExecutor = mock(ExecutorService.class);
        clientWatchdog = mock(ClientWatchdog.class);
        hostManager = spy(new HostManager(executorService, false, clientExecutor, maintenanceExecutor, clientWatchdog));

        host = mock(Host.class);
        serverSocketChannel = mock(ServerSocketChannel.class);
        doReturn(serverSocketChannel).when(host).getServerSocketChannel();

        socketChannel = mock(SocketChannel.class);
        doReturn(socketChannel).when(serverSocketChannel).accept();

        acceptResult = mock(Future.class);
        doReturn(acceptResult).when(clientExecutor).submit(any(NewClientAcceptTask.class));

        doNothing().when(hostManager).configure(socketChannel);
        doNothing().when(hostManager).register(socketChannel, acceptResult);
        doNothing().when(hostManager).closeClientChannel(socketChannel);

        client = mock(Client.class);
        doReturn(client).when(acceptResult).get();
        doReturn(true).when(acceptResult).isDone();
        doReturn(clientExecutor).when(client).getTaskExecutor();
        doReturn(socketChannel).when(client).getChannel();
        doReturn(mock(Lock.class)).when(client).getReadBufferLock();
        doReturn(mock(Lock.class)).when(client).getWriteBufferLock();

        readBuffer = mock(ByteBuffer.class);
        doReturn(readBuffer).when(client).getReadBuffer();

    }

    @AfterEach
    void after() throws IOException {
        if (hostManager.selector != null) {
            hostManager.selector.close();
        }

        Metrics.getRegistry().removeMatching(MetricFilter.ALL);
    }

    @Test
    void whenShutdown_thenCloseSelector() throws IOException, InterruptedException {
        hostManager.selector = Selector.open();
        hostManager.shutdown();
        assertFalse(hostManager.selector.isOpen());
    }

    @Test
    void givenManagerIsStarted_whenNewHost_thenException() throws IOException {
        hostManager.started = true;

        HostManagerIsRunningException e = assertThrows(HostManagerIsRunningException.class, () -> hostManager.newHost("test", 1999, mock(ClientFactory.class)));
        assertThat(e.getMessage()).isEqualTo("Host Manager is running. Please submit your hosts before starting Host Manager's thread!");
    }

    @Test
    void whenNewHostManager_thenHostListIsEmpty() {
        assertThat(hostManager.hosts).isEmpty();
    }

    @Test
    void whenNewHost_thenAddToList() {
        hostManager.newHost("Test", 1234, mock(ClientFactory.class));
        assertThat(hostManager.hosts).hasSize(1);

        Host host = hostManager.hosts.get(0);

        assertEquals("Test", host.getName());
        assertEquals(1234, host.getPort());
    }

    @Test
    void givenHostsRegistered_whenStartHosts_thenCallFactoryAndAssignResultToHost() throws IOException {
        ServerSocketChannel factoryProduct = mock(ServerSocketChannel.class);
        doReturn(factoryProduct).when(hostManager).serverSocketChannelFactory(any(Host.class));

        Host host1 = new Host("Host 1", 1234, mock(ClientFactory.class));
        Host host2 = new Host("Host 2", 1235, mock(ClientFactory.class));

        hostManager.hosts.add(host1);
        hostManager.hosts.add(host2);

        hostManager.startHosts();

        verify(hostManager).serverSocketChannelFactory(host1);
        verify(hostManager).serverSocketChannelFactory(host2);

        assertThat(host1.getServerSocketChannel()).isSameAs(factoryProduct);
        assertThat(host2.getServerSocketChannel()).isSameAs(factoryProduct);
    }

    @Test
    void factoryThrowsException_catchIt() throws IOException {
        doThrow(RuntimeException.class).when(hostManager).serverSocketChannelFactory(any());

        Host host1 = new Host("Host 1", 1234, mock(ClientFactory.class));
        hostManager.hosts.add(host1);

        hostManager.startHosts();
        // no exception
    }

    @Test
    void givenChannelThrowException_whenShutdown_thenCloseAll() throws IOException {
        hostManager.selector = mock(Selector.class);

        SelectionKey key1 = mock(SelectionKey.class);
        SelectionKey key2 = mock(SelectionKey.class);
        SelectionKey key3 = mock(SelectionKey.class);

        SelectableChannel channel1 = mock(SelectableChannel.class);
        SelectableChannel channel2 = mock(SelectableChannel.class);
        SelectableChannel channel3 = mock(SelectableChannel.class);

        doReturn(channel1).when(key1).channel();
        doReturn(channel2).when(key2).channel();
        doReturn(channel3).when(key3).channel();

        doThrow(RuntimeException.class).when(hostManager).closeChannel(channel2);

        Set<SelectionKey> selectionKeySet = new HashSet<>();
        selectionKeySet.add(key1);
        selectionKeySet.add(key2);
        selectionKeySet.add(key3);

        doReturn(selectionKeySet).when(hostManager.selector).keys();

        hostManager.shutdown();

        verify(hostManager).closeChannel(channel1);
        verify(hostManager).closeChannel(channel2);
        verify(hostManager).closeChannel(channel3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenAccept_thenSubmitNewTask() {
        hostManager.accept(serverSocketChannel, host);
        verify(clientExecutor).submit(any(NewClientAcceptTask.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenRead_thenSubmitNewTask() throws ExecutionException, InterruptedException {
        hostManager.read(acceptResult, socketChannel);
        verify(clientExecutor).submit(any(ClientRequestTask.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenAccept_thenConfigureSocketAndRegisterSelector() throws IOException {
        InOrder inOrder = inOrder(hostManager, clientExecutor);

        hostManager.accept(serverSocketChannel, host);

        inOrder.verify(hostManager).configure(socketChannel);
        inOrder.verify(clientExecutor).submit(any(NewClientAcceptTask.class));
        inOrder.verify(hostManager).register(socketChannel, acceptResult);
    }

    @Test
    void givenConfigureThrowsException_whenAccept_thenCloseSocket() throws IOException {
        doThrow(RuntimeException.class).when(hostManager).configure(socketChannel);
        hostManager.accept(serverSocketChannel, host);
        verify(hostManager).closeClientChannel(socketChannel);
    }

    @Test
    void givenRegisterThrowsException_whenAccept_thenCloseSocket() throws IOException {
        doThrow(RuntimeException.class).when(hostManager).register(socketChannel, acceptResult);
        hostManager.accept(serverSocketChannel, host);
        verify(hostManager).closeClientChannel(socketChannel);
    }

    @Test
    void givenAcceptThrowsException_whenAccept_thenDoNotCallClose() throws IOException {
        doThrow(RuntimeException.class).when(serverSocketChannel).accept();
        hostManager.accept(serverSocketChannel, host);
        verify(hostManager, never()).closeClientChannel(any());
    }

    @Test
    void givenClientFutureIsNotCompleted_whenRead_thenDoNotCallGet() throws ExecutionException, InterruptedException {
        doReturn(false).when(acceptResult).isDone();
        hostManager.read(acceptResult, socketChannel);
        verify(acceptResult, never()).get();
    }

    @Test
    void givenClientFutureThrowsException_whenRead_thenCatchIt() throws ExecutionException, InterruptedException {
        doThrow(RuntimeException.class).when(acceptResult).get();
        hostManager.read(acceptResult, socketChannel); // no exception
    }

    @Test
    void givenClientFutureThrowsException_whenRead_thenCloseChannel() throws ExecutionException, InterruptedException, IOException {
        doThrow(RuntimeException.class).when(acceptResult).get();
        hostManager.read(acceptResult, socketChannel);
        verify(hostManager).closeClientChannel(socketChannel);
    }

    @Test
    void whenRead_thenPrepareBuffer() throws Exception {
        ByteBuffer readBuffer = mock(ByteBuffer.class);

        InOrder inOrder = inOrder(readBuffer, socketChannel);
        hostManager.readToBuffer(readBuffer, socketChannel);

        inOrder.verify(readBuffer).compact();
        inOrder.verify(socketChannel).read(readBuffer);
        inOrder.verify(readBuffer).flip();
    }

    @Test
    void givenChannelThrowsException_whenReceive_thenCallFlipInFinallyBlock() throws Exception {
        ByteBuffer readBuffer = mock(ByteBuffer.class);

        doThrow(RuntimeException.class).when(socketChannel).read(readBuffer);

        assertThrows(RuntimeException.class, () -> hostManager.readToBuffer(readBuffer, socketChannel));

        verify(readBuffer).flip();
    }

    @Test
    void whenRead_thenLockReadBufferAndCallReadToBuffer() throws IOException {
        hostManager.read(acceptResult, socketChannel);

        InOrder inOrder = inOrder(hostManager, client.getReadBufferLock());

        inOrder.verify(client.getReadBufferLock()).lock();
        inOrder.verify(hostManager).readToBuffer(readBuffer, socketChannel);
        inOrder.verify(client.getReadBufferLock()).unlock();
    }

    @Test
    void givenReadFromBufferThrowsException_whenRead_thenSubmitShutdownTaskAndUnlockReadBufferLock() throws IOException {
        doThrow(RuntimeException.class).when(hostManager).readToBuffer(readBuffer, socketChannel);
        hostManager.read(acceptResult, socketChannel);
        verify(clientExecutor).submit(any(ClientShutdownTask.class));
        verify(client.getReadBufferLock()).unlock();
    }

    @Test
    void givenManageExecutorServiceIsTrue_whenRun_thenManageExecutor() throws InterruptedException {
        HostManager hostManager = new HostManager(executorService, true);
        ExecutorService tmp = Executors.newSingleThreadExecutor();
        tmp.execute(hostManager);

        hostManager.awaitStart(1, TimeUnit.SECONDS);
        tmp.shutdownNow();
        hostManager.awaitTermination(1, TimeUnit.SECONDS);
        verify(executorService).shutdownNow();
        verify(executorService).awaitTermination(anyLong(), any());
    }

    @Test
    void giveManageExecutorServiceIsFalse_whenRun_thenDoNotManageExecutor() throws InterruptedException {
        HostManager hostManager = new HostManager(executorService, false);
        ExecutorService tmp = Executors.newSingleThreadExecutor();
        tmp.execute(hostManager);

        hostManager.awaitStart(1, TimeUnit.SECONDS);
        tmp.shutdownNow();
        hostManager.awaitTermination(1, TimeUnit.SECONDS);
        verify(executorService, never()).shutdownNow();
    }

    @Test
    void whenManagerIsShutdown_thenShutdownMaintenanceExecutor() throws InterruptedException {
        ExecutorService tmp = Executors.newSingleThreadExecutor();
        tmp.execute(hostManager);

        hostManager.awaitStart(1, TimeUnit.SECONDS);
        tmp.shutdownNow();
        hostManager.awaitTermination(1, TimeUnit.SECONDS);

        verify(maintenanceExecutor).shutdownNow();
        verify(maintenanceExecutor).awaitTermination(anyLong(), any());
    }

    @Test
    void whenStartHostManager_thenStartWatchdog() throws InterruptedException {
        ExecutorService tmp = Executors.newSingleThreadExecutor();
        tmp.execute(hostManager);
        hostManager.awaitStart(1, TimeUnit.SECONDS);

        verify(maintenanceExecutor).execute(clientWatchdog);

        tmp.shutdownNow();
        hostManager.awaitTermination(1, TimeUnit.SECONDS);
    }

}
