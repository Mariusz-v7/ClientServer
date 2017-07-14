package pl.mrugames.client_server.client;

import java.net.InetAddress;
import java.net.Socket;

public class ClientInfo {
    private final static ThreadLocal<ClientInfo> clientInfo = new ThreadLocal<>();

    public static ClientInfo getClientInfo() {
        return clientInfo.get();
    }

    static void setClientInfo(ClientInfo value) {
        clientInfo.set(value);
    }

    static void destroy() {
        clientInfo.remove();
    }

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
