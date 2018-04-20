package pl.mrugames.nucleus.server.client_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.nucleus.client.Client;
import pl.mrugames.nucleus.common.io.LineReader;
import pl.mrugames.nucleus.common.io.LineWriter;

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

        try (Client<String, String> client = new Client<>(address, port, LineWriter::new, LineReader::new)) {
            System.out.println("READ> " + client.read());

            client.write("Hello World!");
            client.write("TEST FRAME");

            System.out.println("READ> " + client.read());
            System.out.println("READ> " + client.read());

            Thread.sleep(1000);

            client.write("exit");

            Thread.sleep(1000);
        }

    }
}
