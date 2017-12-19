package pl.mrugames.client_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.initializers.Initializer;

import java.nio.channels.SocketChannel;
import java.util.List;

public class ClientV2 {
    private final static Logger logger = LoggerFactory.getLogger(Client.class);

    private final String name;
    private final List<Initializer> initializers;
    private final SocketChannel channel;
//    private final Timer clientLifespanMetric;


    ClientV2(String name, SocketChannel channel, List<Initializer> initializers) {
        this.name = name;
        this.channel = channel;
        this.initializers = initializers;
    }

    synchronized void init() {
        try {
            initializers.forEach(Initializer::run);
        } catch (Exception e) {
            logger.error("[{}] Failed to initialize client", name, e);
            close();
        }
    }

    synchronized void onRequest(byte[] bytes) {

    }

    public void close() {
        try {
            channel.close();
        } catch (Exception e) {
            logger.error("[{}] Failed to close socket channel", name, e);
        }
    }

}
