package pl.mrugames.client_server.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientWatchdogSpec {
    private ClientWatchdog watchdog;
    private ExecutorService executorService;

    @BeforeEach
    void before() throws InterruptedException {
        executorService = Executors.newSingleThreadExecutor();
        watchdog = spy(new ClientWatchdog("Test", 30));
        executorService.execute(watchdog);
        if (!watchdog.awaitStart(30, TimeUnit.SECONDS)) {
            fail("Failed to start");
        }
    }

    @AfterEach
    void after() throws InterruptedException {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            fail("Failed to stop");
        }
    }

    @Test
    void givenInit_thenSemaphoreIsZero() {
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(0);
    }

    @Test
    void whenRegister_thenIncreaseSemaphore() throws InterruptedException {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) { // stop it to prevent decreasing permit
            fail("Failed to stop");
        }

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

    @Test
    void givenNoConnections_whenWatchdogIsRun_thenWait() throws InterruptedException {
        Thread.sleep(250);
        verify(watchdog, never()).check();
    }

    @Test
    void givenConnectionRegistered_whenWatchdogIsRun_thenCallCheck() throws InterruptedException {
        Thread.sleep(100);
        watchdog.register(mock(CommV2.class), mock(Socket.class), "");

        Thread.sleep(100);
        verify(watchdog, times(1)).check();
    }
}
