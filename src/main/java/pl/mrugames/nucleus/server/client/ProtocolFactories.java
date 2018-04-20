package pl.mrugames.nucleus.server.client;

import pl.mrugames.nucleus.common.io.*;
import pl.mrugames.nucleus.server.client.filters.FilterProcessor;
import pl.mrugames.nucleus.server.client.filters.StringToWebSocketFrameFilter;
import pl.mrugames.nucleus.server.client.filters.WebSocketFrameToStringFilter;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProtocolFactories {

    public static List<ProtocolFactory<? extends Serializable, ? extends Serializable>> createProtocolFactoryForWebSocket(String httpProtocolName, String webSocketProtocolName) {
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

    public static List<ProtocolFactory<? extends Serializable, ? extends Serializable>> createProtocolFactoryForObjectSocket(String protocolName) {
        return Collections.singletonList(new ProtocolFactory<>(
                ObjectWriter::new,
                ObjectReader::new,
                FilterProcessor.EMPTY_FILTER_PROCESSOR,
                FilterProcessor.EMPTY_FILTER_PROCESSOR,
                protocolName
        ));
    }
}
