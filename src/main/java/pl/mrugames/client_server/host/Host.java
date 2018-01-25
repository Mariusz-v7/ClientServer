package pl.mrugames.client_server.host;

import pl.mrugames.client_server.client.ClientFactory;

import java.nio.channels.ServerSocketChannel;

class Host {
    private final String name;
    private final int port;
    private final ClientFactory clientFactory;
    private volatile ServerSocketChannel serverSocketChannel;

    Host(String name, int port, ClientFactory clientFactory) {
        this.name = name;
        this.port = port;
        this.clientFactory = clientFactory;
    }

    String getName() {
        return name;
    }

    int getPort() {
        return port;
    }

    ClientFactory getClientFactory() {
        return clientFactory;
    }

    ServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public String toString() {
        return "Host{" +
                "name='" + name + '\'' +
                ", port=" + port +
                ", clientFactory=" + clientFactory +
                '}';
    }
}
