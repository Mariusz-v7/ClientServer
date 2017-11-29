package pl.mrugames.client_server.host;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HostManagerSpec {
    private HostManager hostManager;

    @BeforeEach
    void before() throws IOException {
        hostManager = new HostManager();
    }

    @Test
    void givenNewManager_thenSelectorIsOpen() {
        assertTrue(hostManager.selector.isOpen());
    }

    @Test
    void whenShutdown_thenCloseSelector() throws IOException, InterruptedException {
        hostManager.shutdown();
        assertFalse(hostManager.selector.isOpen());
    }

    @Test
    void givenHostListNotEmpty_whenShutdown_thenCallShutdownAndRemove() throws IOException {
        Host host1 = mock(Host.class);
        Host host2 = mock(Host.class);

        hostManager.hosts.add(host1);
        hostManager.hosts.add(host2);

        hostManager.shutdown();
        verify(host1).shutdown();
        verify(host2).shutdown();

        assertThat(hostManager.hosts).isEmpty();
    }

    @Test
    void givenSelectorIsClosed_whenNewHost_thenException() throws IOException {
        hostManager.selector.close();

        assertThrows(HostManagerIshShutDownException.class, () -> hostManager.newHost("test", 1999, mock(ClientFactory.class)));
    }
}
