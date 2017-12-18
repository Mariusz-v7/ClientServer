package pl.mrugames.client_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketHelper implements AutoCloseable {
    private final Socket client;
    private final Socket server;

    public SocketHelper() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(10000)) {
            client = new Socket("localhost", 10000);
            server = serverSocket.accept();
        }
    }

    public InputStream getInputStream() throws IOException {
        return server.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return client.getOutputStream();
    }

    @Override
    public void close() throws Exception {
        client.close();
        server.close();
    }
}
