package pl.mrugames.client_server.tasks;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class TaskWatchdog implements Runnable {
    final List<TaskData> tasks;
    private final CompletionService executor;

    public TaskWatchdog(CompletionService executor) {
        this.tasks = new CopyOnWriteArrayList<>();
        this.executor = executor;
    }

    void submit(Future<?> request, long timeoutSeconds) {
        tasks.add(new TaskData(request, Instant.now(), timeoutSeconds));
    }

    long getNextPossibleTimeout() {
        Instant now = Instant.now();

        return tasks.stream()
                .mapToLong(task -> getSecondsToTimeout(task.getSubmitted(), now, task.getTimeoutSeconds()))
                .min()
                .orElse(0);
    }

    long getSecondsToTimeout(Instant submitted, Instant now, long timeoutSeconds) {
        long secondsPassed = (now.toEpochMilli() - submitted.toEpochMilli()) / 1000;

        long secondsLeft = timeoutSeconds - secondsPassed;

        return secondsLeft > 0 ? secondsLeft : 0;
    }

    boolean isTimeout(Instant submitted, Instant now, long timeoutSeconds) {
        return getSecondsToTimeout(submitted, now, timeoutSeconds) <= 0;
    }

    @Override
    public void run() {
        //TODO: remove when done or timeout
        //TODO: log exceptions from tasks

        //TODO: break when new task submitted

        while (!Thread.currentThread().isInterrupted()) {

        }
    }
}
