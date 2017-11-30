package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.nio.channels.ServerSocketChannel;

class Host implements Shutdownable {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

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
    public void shutdown() {
        logger.info("[{}] Host is shutting down", name);
        clientFactory.shutdown();
        logger.info("[{}] Host has shutdown", name);
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
