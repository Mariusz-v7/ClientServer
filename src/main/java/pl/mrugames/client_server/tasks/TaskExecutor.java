package pl.mrugames.client_server.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TaskExecutor {
    private final ExecutorService executor;

    public TaskExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public <T> Future<T> submit(Callable<T> task) {
        //TODO: add to timeout watchdog
        //TODO: log exceptions from tasks
        return executor.submit(task);
    }
}
