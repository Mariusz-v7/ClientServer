package pl.mrugames.client_server.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

class Host {
    private final static Logger logger = LoggerFactory.getLogger(Host.class);

    private final String name;
    private final int port;
    private final ClientFactory clientFactory;
    private final ServerSocketChannel serverSocketChannel;

    Host(String name, int port, ClientFactory clientFactory, ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        this.name = name;
        this.port = port;
        this.clientFactory = clientFactory;
        this.serverSocketChannel = serverSocketChannel;

        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        logger.info("New Host has been created: {}", this);
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

    void shutdown() {
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            logger.error("[{}] {}", name, e.getMessage(), e);
        }

        clientFactory.shutdown();
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
