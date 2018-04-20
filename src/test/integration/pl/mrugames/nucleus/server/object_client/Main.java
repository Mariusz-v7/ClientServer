package pl.mrugames.nucleus.server.object_client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.client.Client;
import pl.mrugames.nucleus.common.io.ObjectReader;
import pl.mrugames.nucleus.common.io.ObjectWriter;
import pl.mrugames.nucleus.server.object_server.Frame;

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

        try (Client<Frame, Frame> client = new Client<>(address, port, ObjectWriter::new, ObjectReader::new)) {
            client.write(new Frame("Hello World!"));
            client.write(new Frame("TEST FRAME"));

            System.out.println(client.read());
            System.out.println(client.read());

            client.write(new Frame("shutdown"));
        }
    }
}
