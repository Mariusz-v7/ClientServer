package pl.mrugames.client_server.client;

import com.codahale.metrics.MetricFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.Metrics;

import java.io.IOException;
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
        stop();
        Metrics.getRegistry().removeMatching(MetricFilter.ALL);
    }

    private void stop() throws InterruptedException {
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
        stop(); // stop it to prevent decreasing permit

        watchdog.register(mock(Comm.class), mock(Socket.class), "client");
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(1);
    }

    @Test
    void sendTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(31)).when(comm).getLastDataSent();
        doReturn(Instant.now()).when(comm).getLastDataReceived();

        assertTrue(watchdog.isTimeout(comm, ""));
    }

    @Test
    void receiveTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now()).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(31)).when(comm).getLastDataReceived();

        assertTrue(watchdog.isTimeout(comm, ""));
    }

    @Test
    void noTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now()).when(comm).getLastDataSent();
        doReturn(Instant.now()).when(comm).getLastDataReceived();

        assertFalse(watchdog.isTimeout(comm, ""));
    }

    @Test
    void calculateSecondsToSendTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(10)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(5)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(20);
    }

    @Test
    void calculateSecondsToReceiveTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(5)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(10)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(20);
    }

    @Test
    void givenSameTimeOnSendAndReceive_calculateSecondsToTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm)).isEqualTo(15);
    }

    @Test
    void calculateTimeoutCeiling() {
        Comm comm = mock(Comm.class);
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
        doReturn(true).when(watchdog).isTimeout(any(), any());
        watchdog.register(mock(Comm.class), mock(Socket.class), "");

        Thread.sleep(100);
        verify(watchdog, times(1)).check();
    }

    @Test
    void givenConnectionRegistered_whenIsTimeoutReturnTrue_thenRemove() throws InterruptedException, IOException {
        stop();

        doReturn(true).when(watchdog).isTimeout(any(), any());
        Socket socket = mock(Socket.class);

        watchdog.register(mock(Comm.class), socket, "");

        watchdog.check();

        verify(socket).close();
        assertThat(watchdog.comms).isEmpty();
    }

    @Test
    void givenSocketThrowsException_whenCheck_thenCatchIt() throws InterruptedException, IOException {
        stop();

        doReturn(true).when(watchdog).isTimeout(any(), any());
        Socket socket = mock(Socket.class);
        doThrow(IOException.class).when(socket).close();

        watchdog.register(mock(Comm.class), socket, "");

        watchdog.check();

        verify(socket).close();
        assertThat(watchdog.comms).isEmpty();
    }

    @Test
    void givenAllConnectionsTimedOut_whenCheck_thenReturnMinusOne() throws InterruptedException {
        stop();

        doReturn(true).when(watchdog).isTimeout(any(), any());

        watchdog.register(mock(Comm.class), mock(Socket.class), "");
        watchdog.register(mock(Comm.class), mock(Socket.class), "");

        assertThat(watchdog.check()).isEqualTo(-1);
    }

    @Test
    void givenConnectionsNotTimedOut_whenCheck_thenReturnSoonestPossibleTimeout() throws InterruptedException {
        stop();

        doReturn(true, false, false).when(watchdog).isTimeout(any(), any());

        Comm comm1 = mock(Comm.class);
        Comm comm2 = mock(Comm.class);
        Comm comm3 = mock(Comm.class);

        watchdog.register(comm1, mock(Socket.class), "");
        watchdog.register(comm2, mock(Socket.class), "");
        watchdog.register(comm3, mock(Socket.class), "");

        doReturn(30L).when(watchdog).calculateSecondsToTimeout(comm2);
        doReturn(20L).when(watchdog).calculateSecondsToTimeout(comm3);

        assertThat(watchdog.check()).isEqualTo(20);
    }

    @Test
    void givenConnectionNotTimedOut_whenWatchdogIsRun_thenWaitGivenAmountOfSeconds() throws InterruptedException {
        doReturn(false).when(watchdog).isTimeout(any(), any());
        doReturn(1L).when(watchdog).calculateSecondsToTimeout(any());

        watchdog.register(mock(Comm.class), mock(Socket.class), "");

        Thread.sleep(550);
        verify(watchdog, times(1)).check();

        Thread.sleep(550);
        verify(watchdog, times(2)).check();
    }

    @Test
    void givenConnectionNotTimedOut_whenWatchdogIsRun_thenNextCallIsWhenNewConnectionIsRegistered() throws InterruptedException {
        doReturn(false).when(watchdog).isTimeout(any(), any());
        doReturn(10L).when(watchdog).calculateSecondsToTimeout(any());

        watchdog.register(mock(Comm.class), mock(Socket.class), "");

        Thread.sleep(550);
        verify(watchdog, times(1)).check();

        watchdog.register(mock(Comm.class), mock(Socket.class), "");

        Thread.sleep(550);
        verify(watchdog, times(2)).check();
    }
}
