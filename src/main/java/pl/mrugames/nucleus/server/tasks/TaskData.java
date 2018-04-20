package pl.mrugames.nucleus.server.tasks;

import java.time.Instant;
import java.util.concurrent.Future;

public class TaskData {
    private final Future result;
    private final Instant submitted;
    private final long timeoutSeconds;

    public TaskData(Future result, Instant submitted, long timeoutSeconds) {
        this.result = result;
        this.submitted = submitted;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Future getResult() {
        return result;
    }

    public Instant getSubmitted() {
        return submitted;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public String toString() {
        return "TaskData{" +
                "result=" + result +
                ", submitted=" + submitted +
                ", timeoutSeconds=" + timeoutSeconds +
                '}';
    }
}
