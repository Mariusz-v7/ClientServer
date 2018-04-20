package pl.mrugames.nucleus.client;

import java.io.IOException;
import java.net.Socket;

public class Client implements AutoCloseable {
    private final Socket socket;

    public Client(String address, int port) throws IOException {
        this.socket = new Socket(address, port);
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
