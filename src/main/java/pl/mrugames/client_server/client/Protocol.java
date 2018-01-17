package pl.mrugames.client_server.client;

import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.Serializable;

public class Protocol<Reader extends Serializable, Writer extends Serializable> {
    private final ClientWriter<Writer> clientWriter;
    private final ClientReader<Reader> clientReader;
    private final FilterProcessor inputFilterProcessor;
    private final FilterProcessor outputFilterProcessor;

    public Protocol(ClientWriter<Writer> clientWriter,
                    ClientReader<Reader> clientReader,
                    FilterProcessor inputFilterProcessor,
                    FilterProcessor outputFilterProcessor) {
        this.clientWriter = clientWriter;
        this.clientReader = clientReader;
        this.inputFilterProcessor = inputFilterProcessor;
        this.outputFilterProcessor = outputFilterProcessor;
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
}
