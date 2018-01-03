package pl.mrugames.client_server.client.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.IOExceptionWrapper;
import pl.mrugames.client_server.websocket.WebSocketHandshakeParser;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class WebSocketInitializer /*implements Initializer */ {
    private final static Logger logger = LoggerFactory.getLogger(WebSocketInitializer.class);

    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final WebSocketHandshakeParser parser;

    public static BiFunction<InputStream, OutputStream, Initializer> create(WebSocketHandshakeParser parser) {
//        return ((inputStream, outputStream) -> new WebSocketInitializer(inputStream, outputStream, parser));
        return null;//TODO
    }

    //TODO: use the same  buffers as normal comms
    private WebSocketInitializer(InputStream reader, OutputStream writer, WebSocketHandshakeParser parser) {
        this.reader = new BufferedReader(new InputStreamReader(reader));
        this.writer = new BufferedWriter(new OutputStreamWriter(writer));
        this.parser = parser;
    }

    @Deprecated
    public void run() {  // TODO: nonblocking
        logger.info("Initializing WebSocket handshake procedure.");
        List<String> requestLines = new LinkedList<>();

        try {
            do {
                requestLines.add(reader.readLine());
            } while (reader.ready());

            logger.info("Handshake request received.");

            String request = requestLines.stream().collect(Collectors.joining("\r\n"));
            writer.write(parser.parse(request));
            writer.flush();

            logger.info("Handshake response sent.");
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }
    }
}
