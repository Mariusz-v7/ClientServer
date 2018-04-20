package pl.mrugames.nucleus.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketHelper implements AutoCloseable {
    private final SocketChannel client;
    private final SocketChannel server;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(1024);

    public SocketHelper() throws IOException {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new InetSocketAddress("localhost", 10000));

            client = SocketChannel.open(new InetSocketAddress("localhost", 10000));
            client.configureBlocking(false);

            server = serverSocketChannel.accept();
            server.configureBlocking(false);

            readBuffer.flip();
        }
    }

    public int flush() throws IOException {
        writeBuffer.flip();
        int result = client.write(writeBuffer);

        writeBuffer.compact();

        read();

        return result;
    }

    public int write(byte... bytes) throws IOException {
        writeBuffer.put(bytes);
        return flush();
    }

    public int writeInt(int num) throws IOException {
        writeBuffer.putInt(num);
        return flush();
    }

    public int writeShort(short num) throws IOException {
        writeBuffer.putShort(num);
        return flush();
    }

    public ByteBuffer read() throws IOException {
        readBuffer.compact();

        server.read(readBuffer);

        readBuffer.flip();

        return readBuffer;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    @Override
    public void close() throws Exception {
        client.close();
        server.close();
    }
}
