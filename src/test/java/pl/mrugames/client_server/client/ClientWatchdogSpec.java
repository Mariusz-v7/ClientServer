package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

class ClientWatchdogSpec {
    private ClientWatchdog watchdog;

    @BeforeEach
    void before() {
        watchdog = new ClientWatchdog("Test");
    }

    @Test
    void givenInit_thenSemaphoreIsZero() {
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(0);
    }

    @Test
    void whenRegister_thenIncreaseSemaphore() {
        watchdog.register(mock(CommV2.class), mock(Socket.class));
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(1);
    }
}
