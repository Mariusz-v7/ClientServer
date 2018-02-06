package pl.mrugames.client_server.tasks;

import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CompletionService;

import static org.mockito.Mockito.mock;

class TaskExecutorSpec {
    private TaskExecutor taskExecutor;
    private CompletionService completionService;

    @BeforeEach
    void before() {
        completionService = mock(CompletionService.class);
        taskExecutor = new TaskExecutor(completionService);
    }
}
