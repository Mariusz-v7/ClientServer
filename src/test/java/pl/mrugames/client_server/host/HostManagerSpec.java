package pl.mrugames.client_server.host;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.ClientFactoryV2;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HostManagerSpec {
    private HostManager hostManager;

    @BeforeEach
    void before() throws IOException, InterruptedException {
        hostManager = spy(new HostManager());
    }

    @AfterEach
    void after() throws IOException {
        if (hostManager.selector != null) {
            hostManager.selector.close();
        }
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

        HostManagerIsRunningException e = assertThrows(HostManagerIsRunningException.class, () -> hostManager.newHost("test", 1999, mock(ClientFactoryV2.class)));
        assertThat(e.getMessage()).isEqualTo("Host Manager is running. Please submit your hosts before starting Host Manager's thread!");
    }

    @Test
    void whenNewHostManager_thenHostListIsEmpty() {
        assertThat(hostManager.hosts).isEmpty();
    }

    @Test
    void whenNewHost_thenAddToList() {
        hostManager.newHost("Test", 1234, mock(ClientFactoryV2.class));
        assertThat(hostManager.hosts).hasSize(1);

        Host host = hostManager.hosts.get(0);

        assertEquals("Test", host.getName());
        assertEquals(1234, host.getPort());
    }

    @Test
    void givenHostsRegistered_whenStartHosts_thenCallFactoryAndAssignResultToHost() throws IOException {
        ServerSocketChannel factoryProduct = mock(ServerSocketChannel.class);
        doReturn(factoryProduct).when(hostManager).serverSocketChannelFactory(any(Host.class));

        Host host1 = new Host("Host 1", 1234, mock(ClientFactoryV2.class));
        Host host2 = new Host("Host 2", 1235, mock(ClientFactoryV2.class));

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

        Host host1 = new Host("Host 1", 1234, mock(ClientFactoryV2.class));
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

        doThrow(RuntimeException.class).when(hostManager).closeChannel(key2);

        Set<SelectionKey> selectionKeySet = new HashSet<>();
        selectionKeySet.add(key1);
        selectionKeySet.add(key2);
        selectionKeySet.add(key3);

        doReturn(selectionKeySet).when(hostManager.selector).keys();

        hostManager.shutdown();

        verify(hostManager).closeChannel(key1);
        verify(hostManager).closeChannel(key2);
        verify(hostManager).closeChannel(key3);
    }

    @Test
    void givenFactoryThrowsException_whenClientAccept_thenCloseSocket() throws Exception {
        Host host = mock(Host.class);
        ClientFactoryV2 clientFactory = mock(ClientFactoryV2.class);
        doThrow(RuntimeException.class).when(clientFactory).create(any());

        doReturn(clientFactory).when(host).getClientFactory();

        Socket socket = mock(Socket.class);

        doReturn(socket).when(hostManager).accept(any(), any());

        hostManager.acceptConnection(host, mock(ServerSocketChannel.class));

        verify(socket).close();
    }
}
