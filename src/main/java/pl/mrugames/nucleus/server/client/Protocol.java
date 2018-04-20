package pl.mrugames.nucleus.server.client;

import pl.mrugames.nucleus.common.io.ClientReader;
import pl.mrugames.nucleus.common.io.ClientWriter;
import pl.mrugames.nucleus.server.client.filters.FilterProcessor;

import java.io.Serializable;

public class Protocol<Reader extends Serializable, Writer extends Serializable> {
    private final ClientWriter<Writer> clientWriter;
    private final ClientReader<Reader> clientReader;
    private final FilterProcessor inputFilterProcessor;
    private final FilterProcessor outputFilterProcessor;
    private final String name;

    public Protocol(ClientWriter<Writer> clientWriter,
                    ClientReader<Reader> clientReader,
                    FilterProcessor inputFilterProcessor,
                    FilterProcessor outputFilterProcessor,
                    String name) {
        this.clientWriter = clientWriter;
        this.clientReader = clientReader;
        this.inputFilterProcessor = inputFilterProcessor;
        this.outputFilterProcessor = outputFilterProcessor;
        this.name = name;
    }

    public ClientWriter<Writer> getClientWriter() {
        return clientWriter;
    }

    public ClientReader<Reader> getClientReader() {
        return clientReader;
    }

    public FilterProcessor getInputFilterProcessor() {
        return inputFilterProcessor;
    }

    public FilterProcessor getOutputFilterProcessor() {
        return outputFilterProcessor;
    }

    public String getName() {
        return name;
    }
}
