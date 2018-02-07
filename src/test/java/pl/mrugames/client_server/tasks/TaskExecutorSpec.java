package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CompletionService;

import static org.mockito.Mockito.mock;

class TaskExecutorSpec {
    private TaskExecutor taskExecutor;
    private CompletionService completionService;
    private TaskWatchdog taskWatchdog;

    @BeforeEach
    void before() {
        completionService = mock(CompletionService.class);
        taskWatchdog = mock(TaskWatchdog.class);
        taskExecutor = new TaskExecutor(completionService, taskWatchdog);
    }
}
