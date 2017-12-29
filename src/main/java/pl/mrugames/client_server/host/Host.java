package pl.mrugames.client_server.host;

import pl.mrugames.client_server.client.ClientFactory;
import pl.mrugames.client_server.tasks.TaskExecutor;

import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;

class Host {
    private final String name;
    private final int port;
    private final ClientFactory clientFactory;
    private final TaskExecutor taskExecutor;
    private volatile ServerSocketChannel serverSocketChannel;

    Host(String name, int port, ClientFactory clientFactory, ExecutorService taskExecutor) {
        this.name = name;
        this.port = port;
        this.clientFactory = clientFactory;
        this.taskExecutor = new TaskExecutor(taskExecutor);
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

    TaskExecutor getTaskExecutor() {
        return taskExecutor;
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
