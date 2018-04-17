package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.client_server.client.filters.WebSocketFrameToStringFilter;
import pl.mrugames.client_server.client.io.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProtocolFactories {

    public static List<ProtocolFactory<?, ?>> createProtocolFactoryForWebSocket(String httpProtocolName, String webSocketProtocolName) {
        List<ProtocolFactory<?, ?>> protocolFactories = new LinkedList<>();

        protocolFactories.add(
                new ProtocolFactory<>(TextWriter::new, TextReader::new, FilterProcessor.EMPTY_FILTER_PROCESSOR, FilterProcessor.EMPTY_FILTER_PROCESSOR, httpProtocolName)
        );

        protocolFactories.add(
                new ProtocolFactory<>(WebSocketWriter::new, WebSocketReader::new,
                        FilterProcessor.oneFilterFactory(WebSocketFrameToStringFilter.getInstance()),
                        FilterProcessor.oneFilterFactory(StringToWebSocketFrameFilter.getInstance()),
                        webSocketProtocolName)
        );

        return Collections.unmodifiableList(protocolFactories);
    }

    public static List<ProtocolFactory<?, ?>> createProtocolFactoryForObjectSocket(String protocolName) {
        return Collections.singletonList(new ProtocolFactory<>(
                ObjectWriter::new,
                ObjectReader::new,
                FilterProcessor.EMPTY_FILTER_PROCESSOR,
                FilterProcessor.EMPTY_FILTER_PROCESSOR,
                protocolName
        ));
    }
}
