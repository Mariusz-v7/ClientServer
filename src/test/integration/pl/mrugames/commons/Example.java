package pl.mrugames.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientFactory;
import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.ClientWorkerFactory;
import pl.mrugames.commons.client.io.TextClientReader;
import pl.mrugames.commons.client.io.TextClientWriter;
import pl.mrugames.commons.host.Host;

import java.util.concurrent.TimeUnit;

public class Example {
    private final static Logger logger = LoggerFactory.getLogger(Example.class);
    private static Host host;

    public static void main(String ...args) throws InterruptedException {
        logger.info("Example started...");


        ClientFactory clientFactory = new ClientFactory<>(
                "Example Client",
                20,
                60,
                TextClientWriter::getInstance,
                TextClientReader::getInstance,
                createWorkerFactory()
        );

        host = new Host("Example Host", 10000, clientFactory);

        host.start();
        host.join();

        logger.info("Example finished...");
    }

    private static ClientWorkerFactory<String, String> createWorkerFactory() {
        return (name, comm, shutdownSwitch) -> new ClientWorker() {
            @Override
            public void onClientDown() {
                logger.info("Client down");
            }

            @Override
            public void run() {
                logger.info("Client started");

                comm.send("Hello\n\nSend exit to exit\nSend shutdown to kill host \n\n");

                String str;

                do {
                    try {
                        str = comm.receive(30, TimeUnit.SECONDS);

                        logger.info("Message received: {}", str);
                        comm.send("Thank you. your message: " + str + "\n");

                        if (str.equals("shutdown")) {
                            host.interrupt();
                        }
                    } catch (InterruptedException e) {
                        logger.error("Timeout.");
                        break;
                    }
                } while (!str.equals("exit") && !Thread.currentThread().isInterrupted());

                comm.send("Good bye!");

                logger.info("Client finished");

                shutdownSwitch.run();
            }
        };
    }
}
