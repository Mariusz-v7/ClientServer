package pl.mrugames.commons.host;

import java.net.Socket;

public interface ClientFactory {
    void create(Socket socket);
}
