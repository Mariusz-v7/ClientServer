package pl.mrugames.commons.client;

import java.net.Socket;

public interface ClientFactory {
    void create(Socket socket);
}
