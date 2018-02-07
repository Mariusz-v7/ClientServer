package pl.mrugames.client_server.tasks;

import java.util.concurrent.Future;

public class TaskWatchdog implements Runnable {

    void submit(Future<?> request, long timeoutSeconds) {

    }

    @Override
    public void run() {
        //TODO: add to timeout watchdog
        //TODO: log exceptions from tasks

    }
}
