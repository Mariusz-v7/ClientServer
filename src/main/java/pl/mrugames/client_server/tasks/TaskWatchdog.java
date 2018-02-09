package pl.mrugames.client_server.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TaskWatchdog implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    final List<TaskData> tasks;
    private final CompletionService completionService;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final CountDownLatch stopSignal = new CountDownLatch(1);
    private final Semaphore taskCount = new Semaphore(0);

    public TaskWatchdog(CompletionService completionService) {
        this.tasks = new CopyOnWriteArrayList<>();
        this.completionService = completionService;
    }

    synchronized void submit(Future<?> request, long timeoutSeconds) {
        tasks.add(new TaskData(request, Instant.now(), timeoutSeconds));
        taskCount.release();
    }

    long getNextPossibleTimeout() {
        Instant now = Instant.now();

        return tasks.stream()
                .mapToLong(task -> getSecondsToTimeout(task.getSubmitted(), now, task.getTimeoutSeconds()))
                .min()
                .orElse(0);
    }

    List<TaskData> getTimedOutList(Instant now) {
        return tasks.stream()
                .filter(task -> isTimeout(task.getSubmitted(), now, task.getTimeoutSeconds()))
                .collect(Collectors.toList());
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
        startSignal.countDown();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                logger.info("Awaiting for tasks.");

                taskCount.acquire(); // wait for tasks amount not 0
                taskCount.release(); // restore tasks amount

                logger.info("There are {} tasks, starting cycle", tasks.size());

                cycle();

                logger.info("There are {} tasks, after cycle", tasks.size());
            } catch (InterruptedException e) {
                logger.info("Watchdog interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error during clean up: " + e.getMessage(), e);
            }
        }

        stopSignal.countDown();
    }

    void cycle() throws InterruptedException {
        long seconds = getNextPossibleTimeout();

        logger.info("Next possible timeout in: {} seconds", seconds);

        if (seconds > 0) {
            if (completionService.poll(seconds, TimeUnit.SECONDS) != null) {
                logger.info("Task has been finished before timeout, running checks");
            }
        }

        Instant now = Instant.now();
        removeTimedOut(now);
        removeDone();
    }

    synchronized void removeDone() throws InterruptedException {
        List<TaskData> completed = tasks.stream()
                .filter(task -> task.getResult().isDone())
                .collect(Collectors.toList());

        for (TaskData task : completed) {
            try {
                task.getResult().get();
            } catch (Exception e) {
                logger.error("Task finished with exception: {}", task, e);
            } finally {
                removeTask(task);
            }
        }
    }

    synchronized void removeTimedOut(Instant now) throws InterruptedException {
        List<TaskData> timedOutList = getTimedOutList(now);

        for (TaskData taskData : timedOutList) {
            removeTask(taskData);

            logger.error("Task timed out {}", taskData);

            boolean result = taskData.getResult().cancel(true);
            if (!result) {
                if (taskData.getResult().isDone()) {
                    try {
                        taskData.getResult().get();
                    } catch (Exception e) {
                        logger.error("Task completed with error: ", e);
                    }

                } else {
                    logger.error("Failed to shutdown task: {}", taskData);
                }
            }

        }
    }

    void removeTask(TaskData taskData) throws InterruptedException {
        tasks.remove(taskData);
        taskCount.acquire();
    }

    void awaitStart() throws InterruptedException {
        startSignal.await();
    }

    void awaitStop() throws InterruptedException {
        stopSignal.await();
    }
}
