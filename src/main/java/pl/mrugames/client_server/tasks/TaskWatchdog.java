package pl.mrugames.client_server.tasks;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class TaskWatchdog implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    final List<TaskData> tasks;
    private final CompletionService completionService;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final CountDownLatch stopSignal = new CountDownLatch(1);
    private final Semaphore taskCount = new Semaphore(0);

    private final Counter finishedTasks;
    private final Counter failedTasks;
    private final Counter timedOutTasks;
    private final Histogram taskDurations;
    private final Counter tasksSubmitted;
    private final Meter cleanUpCycles;

    private volatile Instant lastCycle;

    public TaskWatchdog(CompletionService completionService) {
        this.tasks = new CopyOnWriteArrayList<>();
        this.completionService = completionService;

        Metrics.getRegistry().register(name(TaskWatchdog.class, "pending_tasks"), (Gauge<Integer>) tasks::size);
        finishedTasks = Metrics.getRegistry().counter(name(TaskWatchdog.class, "finished_tasks"));
        failedTasks = Metrics.getRegistry().counter(name(TaskWatchdog.class, "failed_tasks"));
        timedOutTasks = Metrics.getRegistry().counter(name(TaskWatchdog.class, "timed_out_tasks"));
        taskDurations = Metrics.getRegistry().histogram(name(TaskWatchdog.class, "tasks_durations"));
        tasksSubmitted = Metrics.getRegistry().counter(name(TaskWatchdog.class, "tasks_submitted"));
        cleanUpCycles = Metrics.getRegistry().meter(name(TaskWatchdog.class, "cleanup_cycles"));

        Metrics.getHealthCheckRegistry().register(name(TaskWatchdog.class), new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (startSignal.getCount() == 0 && stopSignal.getCount() == 1) {
                    return HealthCheck.Result.healthy("Last cycle: " + lastCycle);
                } else {
                    return HealthCheck.Result.unhealthy("Start signal: " + startSignal.getCount() + ", stop signal: " + stopSignal.getCount() + ", last cycle: " + lastCycle);
                }
            }
        });
    }

    synchronized void submit(Future<?> request, long timeoutSeconds) {
        tasks.add(new TaskData(request, Instant.now(), timeoutSeconds));
        taskCount.release();
        tasksSubmitted.inc();
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
                logger.debug("Awaiting for tasks.");

                taskCount.acquire(); // wait for tasks amount not 0
                taskCount.release(); // restore tasks amount

                logger.debug("There are {} tasks, starting cycle", tasks.size());

                cycle();

                logger.debug("There are {} tasks, after cycle", tasks.size());
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
        lastCycle = Instant.now();
        cleanUpCycles.mark();

        long seconds = getNextPossibleTimeout();

        logger.debug("Next possible timeout in: {} seconds", seconds);

        if (seconds > 0) {
            if (completionService.poll(seconds, TimeUnit.SECONDS) != null) {
                logger.debug("Task has been finished before timeout, running checks");
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
                finishedTasks.inc();
                updateDuration(task);
            } catch (Exception e) {
                logger.error("Task finished with exception: {}", task, e);
                failedTasks.inc();
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
                        finishedTasks.inc();
                        updateDuration(taskData);
                    } catch (Exception e) {
                        logger.error("Task completed with error: ", e);
                        failedTasks.inc();
                    }

                } else {
                    logger.error("Failed to shutdown task: {}", taskData);
                }
            } else {
                timedOutTasks.inc();
            }

        }
    }

    private void updateDuration(TaskData taskData) {
        Instant now = Instant.now();
        long duration = now.toEpochMilli() - taskData.getSubmitted().toEpochMilli();

        taskDurations.update(duration);
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
