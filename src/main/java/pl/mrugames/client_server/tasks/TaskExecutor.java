package pl.mrugames.client_server.tasks;

import java.util.concurrent.*;

public class TaskExecutor {
    private final CompletionService executor;

    public TaskExecutor(ExecutorService executor) {
        this(new ExecutorCompletionService<>(executor));
    }

    TaskExecutor(CompletionService completionService) {
        this.executor = completionService;
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(Callable<T> task) {
        //TODO: add to timeout watchdog
        //TODO: log exceptions from tasks
        return executor.submit(task);
    }
}
