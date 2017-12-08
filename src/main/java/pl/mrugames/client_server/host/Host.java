package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactoryV2;

import java.nio.channels.ServerSocketChannel;

class Host {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final String name;
    private final int port;
    private final ClientFactoryV2 clientFactory;
    private volatile ServerSocketChannel serverSocketChannel;

    Host(String name, int port, ClientFactoryV2 clientFactory) {
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

    ClientFactoryV2 getClientFactory() {
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
