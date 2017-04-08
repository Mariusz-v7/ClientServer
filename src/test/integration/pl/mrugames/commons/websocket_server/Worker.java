package pl.mrugames.commons.websocket_server;

import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.Comm;

import java.util.concurrent.TimeUnit;

public class Worker implements ClientWorker {
    private final Runnable shutdownSwitch;
    private final String name;
    private final Comm<String, String> comm;
    private final Runnable onClientShutDown;

    public Worker(String name, Comm<String, String> comm, Runnable onClientShutDown, Runnable shutdownSwitch) {
        this.shutdownSwitch = shutdownSwitch;
        this.name = name;
        this.comm = comm;
        this.onClientShutDown = onClientShutDown;
    }

    @Override
    public void onClientTermination() {
        onClientShutDown.run();
    }

    @Override
    public void run() {
        try {
            String message;
            do {
                message = comm.receive(30, TimeUnit.SECONDS);

                if (message.equals("shutdown")) {
                    shutdownSwitch.run();
                    break;
                }

                comm.send("Your message was: " + message);
            } while (!Thread.currentThread().isInterrupted() && message != null && !message.equals("shutdown"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
