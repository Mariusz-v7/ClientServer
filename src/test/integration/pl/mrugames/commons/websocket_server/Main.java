package pl.mrugames.commons.websocket_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.commons.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.commons.client.initializers.WebSocketInitializer;
import pl.mrugames.commons.client.io.WebSocketReader;
import pl.mrugames.commons.client.io.WebSocketWriter;
import pl.mrugames.commons.host.Host;
import pl.mrugames.commons.websocket.WebSocketHandshakeParser;

import java.util.Collections;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static Host host;

    public static void main(String... args) throws InterruptedException {
        if (args.length != 1) {
            logger.error("Please provide port");
            return;
        }

        final int port = Integer.valueOf(args[0]);

        logger.info("Main started...");

        ClientFactory clientFactory = new ClientFactory<>(
                "Main Client",
                60,
                WebSocketWriter::new,
                WebSocketReader::new,
                new WebSocketWorkerFactory(Main::shutdown),
                Collections.singletonList(WebSocketInitializer.create(new WebSocketHandshakeParser())),
                Collections.singletonList(WebSocketFrameToStringFilter.getInstance()),
                Collections.singletonList(StringToWebSocketFrameFilter.getInstance())
        );

        host = new Host("Main Host", port, clientFactory);

        host.start();
        host.join();

        logger.info("Main finished...");
    }

    public static void shutdown() {
        host.interrupt();
    }
}
