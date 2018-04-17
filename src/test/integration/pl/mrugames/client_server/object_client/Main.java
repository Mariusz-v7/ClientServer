package pl.mrugames.client_server.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.object_server.Frame;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws Exception {
        if (args.length != 2) {
            logger.error("Please provide address and port");
            return;
        }

        logger.info("Main started...");

        final String address = args[0];
        final int port = Integer.valueOf(args[1]);

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port));

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            write(outputStream, new Frame("Hello World!"));

            System.out.println(read(inputStream));

        } finally {
            socket.close();
        }
    }

    static void write(OutputStream outputStream, Frame frame) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(frame);

        byte[] bytes = byteArrayOutputStream.toByteArray();
        byte[] sizeBytes = ByteBuffer.allocate(4).putInt(bytes.length).array();

        outputStream.write(sizeBytes);
        outputStream.write(bytes);
    }

    static Frame read(InputStream inputStream) throws IOException, ClassNotFoundException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readInt();

        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        return (Frame) objectInputStream.readUnshared();
    }
}
