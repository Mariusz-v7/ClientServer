package pl.mrugames.nucleus.server.tasks;

import com.codahale.metrics.MetricFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.nucleus.server.Metrics;

import java.time.Instant;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TaskWatchdogSpec {
    private TaskWatchdog watchdog;
    private CompletionService completionService;

    @BeforeEach
    void before() {
        completionService = mock(CompletionService.class);
        watchdog = spy(new TaskWatchdog(completionService));
    }

    @AfterEach
    void after() {
        Metrics.getRegistry().removeMatching(MetricFilter.ALL);
    }

    @Test
    void whenCreate_thenListIsEmpty() {
        assertThat(watchdog.tasks).isEmpty();
    }

    @Test
    void whenSubmitTask_thenAddToList() {
        Future future = mock(Future.class);

        Instant before = Instant.now();

        watchdog.submit(future, 10);

        Instant after = Instant.now();

        assertThat(watchdog.tasks).hasSize(1);

        TaskData taskData = watchdog.tasks.get(0);

        assertThat(taskData.getResult()).isSameAs(future);
        assertThat(taskData.getSubmitted()).isBetween(before, after);
        assertThat(taskData.getTimeoutSeconds()).isEqualTo(10);
    }

    @Test
    void shouldProperlyCalculateSecondsToTimeout() {
        Instant submitted = Instant.now();
        Instant now = submitted.plusSeconds(5);

        assertThat(watchdog.getSecondsToTimeout(submitted, now, 15)).isEqualTo(10);
        assertThat(watchdog.getSecondsToTimeout(submitted, now, 10)).isEqualTo(5);
        assertThat(watchdog.getSecondsToTimeout(submitted, now, 5)).isEqualTo(0);
    }

    @Test
    void givenTimeoutPassedSecondAgo_whenGetSecondsToTimeout_thenReturnZero() {
        Instant submitted = Instant.now();
        Instant now = submitted.plusSeconds(5);

        assertThat(watchdog.getSecondsToTimeout(submitted, now, 4)).isEqualTo(0);
    }

    @Test
    void givenThreeTasksInWatchdog_whenGetNextPossibleTimeout_thenReturnSoonestValue() {
        watchdog.tasks.add(mock(TaskData.class));
        watchdog.tasks.add(mock(TaskData.class));
        watchdog.tasks.add(mock(TaskData.class));
        watchdog.tasks.add(mock(TaskData.class));
        watchdog.tasks.add(mock(TaskData.class));
        watchdog.tasks.add(mock(TaskData.class));
        doReturn(10L, 20L, 15L, 5L, 8L, 12L).when(watchdog).getSecondsToTimeout(any(), any(), anyLong());

        assertThat(watchdog.getNextPossibleTimeout()).isEqualTo(5L);
    }

    @Test
    void givenNoTasks_whenGetNextPossibleTimeout_thenReturnZero() {
        assertThat(watchdog.getNextPossibleTimeout()).isEqualTo(0L);
    }

    @Test
    void givenGetSecondsToTimeoutReturnsZero_whenIsTimeout_thenTrue() {
        doReturn(0L).when(watchdog).getSecondsToTimeout(any(), any(), anyLong());
        assertThat(watchdog.isTimeout(Instant.now(), Instant.now(), 0)).isTrue();
    }

    @Test
    void givenGetSecondsToTimeoutReturnsPositiveNumber_whenIsTimeout_thenTrue() {
        doReturn(1L).when(watchdog).getSecondsToTimeout(any(), any(), anyLong());
        assertThat(watchdog.isTimeout(Instant.now(), Instant.now(), 0)).isFalse();
    }
}
