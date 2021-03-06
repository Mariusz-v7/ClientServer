package pl.mrugames.nucleus.server.client;

import com.codahale.metrics.MetricFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.nucleus.server.Metrics;
import pl.mrugames.nucleus.server.tasks.ClientShutdownTask;
import pl.mrugames.nucleus.server.tasks.TaskExecutor;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionWatchdogSpec {
    private ConnectionWatchdog watchdog;
    private ExecutorService executorService;
    private Client client;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void before() throws InterruptedException, IOException {
        client = mock(Client.class);
        doReturn(30L).when(client).getConnectionTimeoutSeconds();
        doReturn(Instant.now()).when(client).getCreated();

        taskExecutor = mock(TaskExecutor.class);
        doReturn(taskExecutor).when(client).getTaskExecutor();

        executorService = Executors.newSingleThreadExecutor();
        watchdog = spy(new ConnectionWatchdog());

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

        watchdog.register(client);
        assertThat(watchdog.semaphore.availablePermits()).isEqualTo(1);
    }

    @Test
    void sendTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(31)).when(comm).getLastDataSent();
        doReturn(Instant.now()).when(comm).getLastDataReceived();

        assertTrue(watchdog.isTimeout(comm, "", 30));
    }

    @Test
    void receiveTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now()).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(31)).when(comm).getLastDataReceived();

        assertTrue(watchdog.isTimeout(comm, "", 30));
    }

    @Test
    void noTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now()).when(comm).getLastDataSent();
        doReturn(Instant.now()).when(comm).getLastDataReceived();

        assertFalse(watchdog.isTimeout(comm, "", 30));
    }

    @Test
    void calculateSecondsToSendTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(10)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(5)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm, 30)).isEqualTo(20);
    }

    @Test
    void calculateSecondsToReceiveTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(5)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(10)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm, 30)).isEqualTo(20);
    }

    @Test
    void givenSameTimeOnSendAndReceive_calculateSecondsToTimeout() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm, 30)).isEqualTo(15);
    }

    @Test
    void calculateTimeoutCeiling() {
        Comm comm = mock(Comm.class);
        doReturn(Instant.now().minusSeconds(29).minusMillis(50)).when(comm).getLastDataSent();
        doReturn(Instant.now().minusSeconds(15)).when(comm).getLastDataReceived();

        assertThat(watchdog.calculateSecondsToTimeout(comm, 30)).isEqualTo(1);
    }

    @Test
    void givenNoConnections_whenWatchdogIsRun_thenWait() throws InterruptedException {
        Thread.sleep(250);
        verify(watchdog, never()).check();
    }

    @Test
    void givenConnectionRegistered_whenWatchdogIsRun_thenCallCheck() throws InterruptedException {
        Thread.sleep(100);
        doReturn(true).when(watchdog).isTimeout(any(), any(), anyLong());
        watchdog.register(client);

        Thread.sleep(100);
        verify(watchdog, times(1)).check();
    }

    @Test
    void givenConnectionRegistered_whenIsTimeoutReturnTrue_thenRemove() throws InterruptedException, IOException {
        stop();

        doReturn(true).when(watchdog).isTimeout(any(), any(), anyLong());

        watchdog.register(client);

        watchdog.check();

        verify(taskExecutor).submit(any(ClientShutdownTask.class), anyLong());
        assertThat(watchdog.clients).isEmpty();
    }

    @Test
    void givenAllConnectionsTimedOut_whenCheck_thenReturnMinusOne() throws InterruptedException {
        stop();

        TaskExecutor taskExecutor = mock(TaskExecutor.class);

        Client client1 = mock(Client.class);
        Client client2 = mock(Client.class);

        doReturn(Instant.now()).when(client1).getCreated();
        doReturn(Instant.now()).when(client2).getCreated();

        doReturn(taskExecutor).when(client1).getTaskExecutor();
        doReturn(taskExecutor).when(client2).getTaskExecutor();

        doReturn(true).when(watchdog).isTimeout(any(), any(), anyLong());

        watchdog.register(client1);
        watchdog.register(client2);

        assertThat(watchdog.check()).isEqualTo(-1);
    }

    @Test
    void givenConnectionsNotTimedOut_whenCheck_thenReturnSoonestPossibleTimeout() throws InterruptedException {
        stop();

        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        doReturn(mock(Future.class)).when(taskExecutor).submit(any(), anyLong());

        doReturn(true, false, false).when(watchdog).isTimeout(any(), any(), anyLong());

        Client client1 = mock(Client.class);
        Client client2 = mock(Client.class);
        Client client3 = mock(Client.class);

        doReturn(30L).when(client1).getConnectionTimeoutSeconds();
        doReturn(30L).when(client2).getConnectionTimeoutSeconds();
        doReturn(30L).when(client3).getConnectionTimeoutSeconds();

        doReturn(Instant.now()).when(client1).getCreated();
        doReturn(Instant.now()).when(client2).getCreated();
        doReturn(Instant.now()).when(client3).getCreated();

        doReturn(taskExecutor).when(client1).getTaskExecutor();
        doReturn(taskExecutor).when(client2).getTaskExecutor();
        doReturn(taskExecutor).when(client3).getTaskExecutor();

        Comm comm1 = mock(Comm.class);
        Comm comm2 = mock(Comm.class);
        Comm comm3 = mock(Comm.class);

        doReturn(comm1).when(client1).getComm();
        doReturn(comm2).when(client2).getComm();
        doReturn(comm3).when(client3).getComm();

        watchdog.register(client1);
        watchdog.register(client2);
        watchdog.register(client3);

        doReturn(30L).when(watchdog).calculateSecondsToTimeout(comm2, 30);
        doReturn(20L).when(watchdog).calculateSecondsToTimeout(comm3, 30);

        assertThat(watchdog.check()).isEqualTo(20);
    }

    @Test
    void givenConnectionNotTimedOut_whenWatchdogIsRun_thenWaitGivenAmountOfSeconds() throws InterruptedException {
        doReturn(false).when(watchdog).isTimeout(any(), any(), anyLong());
        doReturn(1L).when(watchdog).calculateSecondsToTimeout(any(), anyLong());

        watchdog.register(client);

        Thread.sleep(550);
        verify(watchdog, times(1)).check();

        Thread.sleep(550);
        verify(watchdog, times(2)).check();
    }

    @Test
    void givenConnectionNotTimedOut_whenWatchdogIsRun_thenNextCallIsWhenNewConnectionIsRegistered() throws InterruptedException {
        doReturn(false).when(watchdog).isTimeout(any(), any(), anyLong());
        doReturn(10L).when(watchdog).calculateSecondsToTimeout(any(), anyLong());

        watchdog.register(mock(Client.class));

        Thread.sleep(550);
        verify(watchdog, times(1)).check();

        watchdog.register(mock(Client.class));

        Thread.sleep(550);
        verify(watchdog, times(2)).check();
    }
}
