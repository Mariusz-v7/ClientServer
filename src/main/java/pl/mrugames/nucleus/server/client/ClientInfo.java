package pl.mrugames.nucleus.server.client;

import java.net.InetAddress;
import java.net.Socket;

public class ClientInfo {
    private final String name;
    private final InetAddress remote;
    private final int remotePort;
    private final InetAddress local;
    private final int localPort;

    public ClientInfo(String name, Socket socket) {
        this.name = name;
        remote = socket.getInetAddress();
        remotePort = socket.getPort();
        local = socket.getLocalAddress();
        localPort = socket.getLocalPort();
    }

    public String getName() {
        return name;
    }

    public InetAddress getRemote() {
        return remote;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public InetAddress getLocal() {
        return local;
    }

    public int getLocalPort() {
        return localPort;
    }

    @Override
    public String toString() {
        return name + ", connected from: " + remote + ":" + remotePort + ", to: " + local + ":" + localPort + ".";
    }
}
