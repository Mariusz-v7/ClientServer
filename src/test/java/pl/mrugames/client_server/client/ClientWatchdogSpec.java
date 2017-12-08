package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ClientWatchdogSpec {
    private ClientWatchdog watchdog;

    @BeforeEach
    void before() {
        watchdog = new ClientWatchdog("Test", 30);
    }

    @Test
    void givenInit_thenSemaphoreIsZero() {
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(0);
    }

    @Test
    void whenRegister_thenIncreaseSemaphore() {
        watchdog.register(mock(CommV2.class), mock(Socket.class), "client");
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(1);
    }

    @Test
    void sendTimeout() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now().minusSeconds(31)).when(comm).getLastDataSent();
        doReturn(Instant.now()).when(comm).getLastDataReceived();

        assertTrue(watchdog.isTimeout(comm, ""));
    }

    @Test
    void receiveTimeout() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now()).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(31)).when(comm).getLastDataReceived();

        assertTrue(watchdog.isTimeout(comm, ""));
    }

    @Test
    void noTimeout() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now()).when(comm).getLastDataSent();
        doReturn(Instant.now()).when(comm).getLastDataReceived();

        assertFalse(watchdog.isTimeout(comm, ""));
    }

    @Test
    void calculateSecondsToSendTimeout() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now().minusSeconds(10)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(5)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(20);
    }

    @Test
    void calculateSecondsToReceiveTimeout() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now().minusSeconds(5)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(10)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(20);
    }

    @Test
    void givenSameTimeOnSendAndReceive_calculateSecondsToTimeout() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(15);
    }

    @Test
    void calculateTimeoutCeiling() {
        CommV2 comm = mock(CommV2.class);
        doReturn(Instant.now().minusSeconds(29).minusMillis(50)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(1);
    }
}
