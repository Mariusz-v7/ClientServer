package pl.mrugames.nucleus.server.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.common.io.ObjectReader;
import pl.mrugames.nucleus.common.io.ObjectWriter;
import pl.mrugames.nucleus.server.object_server.Frame;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

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

            ObjectWriter<Frame> objectWriter = new ObjectWriter<>(outputStream);
            objectWriter.write(new Frame("Hello World!"));

            ObjectReader<Frame> objectReader = new ObjectReader<>(inputStream);

            System.out.println(objectReader.read());

            objectWriter.write(new Frame("shutdown"));

        } finally {
            socket.close();
        }
    }

    static Frame read(InputStream inputStream) throws IOException, ClassNotFoundException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readInt();

        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        return (Frame) objectInputStream.readUnshared();
    }
}
