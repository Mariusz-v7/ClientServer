package pl.mrugames.client_server.websocket_server;

import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;
import pl.mrugames.client_server.websocket.WebsocketConstatns;

import java.util.concurrent.TimeUnit;

public class Worker implements ClientWorker {
    private final Runnable shutdownSwitch;
    private final Comm<String, String> comm;
    private final Runnable onClientShutDown;

    public Worker(Comm<String, String> comm, Runnable onClientShutDown, Runnable shutdownSwitch) {
        this.shutdownSwitch = shutdownSwitch;
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

                if (message.equals(WebsocketConstatns.WEBSOCKET_CLOSE_FRAME)) {
                    comm.send(WebsocketConstatns.WEBSOCKET_CLOSE_FRAME);
                    break;
                }

                if (message.equals("CLOSE")) { // init close on server side
                    comm.send(WebsocketConstatns.WEBSOCKET_CLOSE_FRAME);
                    continue;
                }

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
