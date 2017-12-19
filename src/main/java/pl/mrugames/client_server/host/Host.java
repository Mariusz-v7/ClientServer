package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;

class Host {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final String name;
    private final int port;
    private final ClientFactory clientFactory;
    private final ExecutorService clientExecutor;
    private volatile ServerSocketChannel serverSocketChannel;

    Host(String name, int port, ClientFactory clientFactory, ExecutorService clientExecutor) {
        this.name = name;
        this.port = port;
        this.clientFactory = clientFactory;
        this.clientExecutor = clientExecutor;
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

    ExecutorService getClientExecutor() {
        return clientExecutor;
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
