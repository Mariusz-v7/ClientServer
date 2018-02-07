package pl.mrugames.client_server.tasks;

import java.util.concurrent.*;

public class TaskExecutor {
    private final CompletionService executor;
    private final TaskWatchdog taskWatchdog;

    public TaskExecutor(ExecutorService executor, TaskWatchdog taskWatchdog) {
        this(new ExecutorCompletionService<>(executor), taskWatchdog);
    }

    TaskExecutor(CompletionService completionService, TaskWatchdog taskWatchdog) {
        this.executor = completionService;
        this.taskWatchdog = taskWatchdog;
    }

    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(Callable<T> task, long timeoutSeconds) {
        Future future = executor.submit(task);

        taskWatchdog.submit(future, timeoutSeconds);

        return future;
    }
}
