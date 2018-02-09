package pl.mrugames.client_server.tasks;

import com.codahale.metrics.MetricFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.Metrics;

import java.time.Duration;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;

class Helper {
    final CountDownLatch finishTask;
    final Future result;

    Helper(CountDownLatch finishTask, Future result) {
        this.finishTask = finishTask;
        this.result = result;
    }
}

class TaskWatchdogTest {
    private CompletionService completionService;
    private TaskWatchdog taskWatchdog;
    private ExecutorService watchdogExecutor;
    private ExecutorService taskExecutor;

    @SuppressWarnings("unchecked")
    private Helper submitTask(long timeoutSeconds) {
        CountDownLatch countDownLatch = new CountDownLatch(1);  // call countdown to finish task

        Future submit = spy(completionService.submit(() -> {
            countDownLatch.await();
            return null;
        }));

        taskWatchdog.submit(submit, timeoutSeconds);

        return new Helper(countDownLatch, submit);
    }

    @BeforeEach
    void before() throws InterruptedException {
        taskExecutor = Executors.newCachedThreadPool();
        completionService = new ExecutorCompletionService(taskExecutor);
        taskWatchdog = spy(new TaskWatchdog(completionService));

        watchdogExecutor = Executors.newSingleThreadExecutor();
        watchdogExecutor.execute(taskWatchdog);

        taskWatchdog.awaitStart();
    }

    @AfterEach
    void after() throws InterruptedException {
        taskExecutor.shutdownNow();
        watchdogExecutor.shutdownNow();

        taskWatchdog.awaitStop();

        Metrics.getRegistry().removeMatching(MetricFilter.ALL);
    }

    @Test
    void givenNoTasksSubmitted_thenShouldWaitUntilNewTaskIsSubmitted() throws InterruptedException {
        TimeUnit.SECONDS.sleep(10);
        verify(taskWatchdog, never()).cycle();

        assertTimeout(Duration.ofSeconds(1), () -> {
            submitTask(10);

            verify(taskWatchdog, atLeastOnce()).cycle();
        });
    }

    @Test
    void givenTaskSubmitted_thenTimeoutAfterGivenTime() throws InterruptedException {
        Helper helper = submitTask(5);

        TimeUnit.MILLISECONDS.sleep(5200);

        verify(helper.result).cancel(true);
        assertThat(taskWatchdog.tasks).isEmpty();
    }

    @Test
    void doNotTimeoutTasksBeforeTimeout() throws InterruptedException {
        Helper helper = submitTask(5);
        TimeUnit.SECONDS.sleep(4);

        verify(helper.result, never()).cancel(true);
        assertThat(taskWatchdog.tasks).hasSize(1);
    }

    @Test
    void doNotClearTimeoutBeforeAnyTaskTimedOut() throws InterruptedException {
        Helper helper = submitTask(5);
        TimeUnit.SECONDS.sleep(4);

        verify(taskWatchdog, never()).removeTimedOut(any());
    }

    @Test
    void clearTimeoutAfterTaskTimeout() throws InterruptedException {
        Helper helper = submitTask(5);
        TimeUnit.MILLISECONDS.sleep(5200);

        verify(taskWatchdog, atLeastOnce()).removeTimedOut(any());
    }
}
