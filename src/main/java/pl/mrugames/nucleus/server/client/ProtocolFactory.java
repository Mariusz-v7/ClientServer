package pl.mrugames.nucleus.server.client;

import pl.mrugames.nucleus.common.io.ClientReader;
import pl.mrugames.nucleus.common.io.ClientWriter;
import pl.mrugames.nucleus.server.client.filters.FilterProcessor;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class ProtocolFactory<Reader extends Serializable, Writer extends Serializable> {
    private final Function<ByteBuffer, ClientWriter<Writer>> clientWriterFactory;
    private final Function<ByteBuffer, ClientReader<Reader>> clientReaderFactory;
    private final FilterProcessor inputFilterProcessor;
    private final FilterProcessor outputFilterProcessor;
    private final String key;

    public ProtocolFactory(Function<ByteBuffer, ClientWriter<Writer>> clientWriterFactory,
                           Function<ByteBuffer, ClientReader<Reader>> clientReaderFactory,
                           FilterProcessor inputFilterProcessor,
                           FilterProcessor outputFilterProcessor,
                           String key) {
        this.clientWriterFactory = clientWriterFactory;
        this.clientReaderFactory = clientReaderFactory;
        this.inputFilterProcessor = inputFilterProcessor;
        this.outputFilterProcessor = outputFilterProcessor;
        this.key = key;
    }

    public Protocol<Reader, Writer> create(ByteBuffer writeBuffer, ByteBuffer readBuffer) {
        ClientWriter<Writer> clientWriter = clientWriterFactory.apply(writeBuffer);
        ClientReader<Reader> clientReader = clientReaderFactory.apply(readBuffer);

        return new Protocol<>(clientWriter, clientReader, inputFilterProcessor, outputFilterProcessor, key);
    }
}
