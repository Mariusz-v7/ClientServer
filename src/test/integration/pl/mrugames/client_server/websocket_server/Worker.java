package pl.mrugames.client_server.websocket_server;

import pl.mrugames.client_server.client.CommV2;
import pl.mrugames.client_server.client.frames.WebSocketFrame;
import pl.mrugames.client_server.websocket.WebsocketConstants;

public class Worker implements Runnable {
    private final CommV2<String, String, WebSocketFrame, WebSocketFrame> comm;
    private final Runnable onClientShutDown;

    public Worker(CommV2<String, String, WebSocketFrame, WebSocketFrame> comm, Runnable onClientShutDown) {
        this.comm = comm;
        this.onClientShutDown = onClientShutDown;
    }

    @Override
    public void run() {
        try {
            String message;
            do {
                message = comm.receive();

                if (message.equals(WebsocketConstants.WEBSOCKET_CLOSE_FRAME)) {
                    comm.send(WebsocketConstants.WEBSOCKET_CLOSE_FRAME);
                    break;
                }

                if (message.equals("CLOSE")) { // init close on server side
                    comm.send(WebsocketConstants.WEBSOCKET_CLOSE_FRAME);
                    continue;
                }

                if (message.equals("shutdown")) {
                    break;
                }

                comm.send("Your message was: " + message);
            } while (!Thread.currentThread().isInterrupted() && message != null && !message.equals("shutdown"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        onClientShutDown.run();
    }
}
