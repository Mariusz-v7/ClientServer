package pl.mrugames.nucleus.server.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TaskExecutorSpec {
    private TaskExecutor taskExecutor;
    private CompletionService completionService;
    private TaskWatchdog taskWatchdog;
    private Future response;
    private Callable<?> task;
    private long timeout = 10;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        completionService = mock(CompletionService.class);
        taskWatchdog = mock(TaskWatchdog.class);
        taskExecutor = new TaskExecutor(completionService, taskWatchdog);

        response = mock(Future.class);
        task = mock(Callable.class);

        doReturn(response).when(completionService).submit(task);
    }

    @Test
    void whenSubmitTask_thenSubmitItToWatchdog() {
        Future<?> submit = taskExecutor.submit(task, timeout);
        verify(taskWatchdog).submit(response, timeout);

        assertThat(submit).isSameAs(response);
    }

}
