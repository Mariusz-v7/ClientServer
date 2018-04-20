package pl.mrugames.nucleus.client;

import pl.mrugames.nucleus.common.io.ClientReader;
import pl.mrugames.nucleus.common.io.ClientWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Function;

public class Client<Write extends Serializable, Read extends Serializable> implements AutoCloseable {
    private final Socket socket;
    private final ClientReader<Read> clientReader;
    private final ClientWriter<Write> clientWriter;

    public Client(String address, int port, Function<OutputStream, ClientWriter<Write>> writerFactory, Function<InputStream, ClientReader<Read>> readerFactory) throws IOException {
        this.socket = new Socket(address, port);
        this.clientReader = readerFactory.apply(socket.getInputStream());
        this.clientWriter = writerFactory.apply(socket.getOutputStream());
    }

    public void write(Write frame) throws Exception {
        clientWriter.write(frame);
    }

    public Read read() throws Exception {
        return clientReader.read();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
