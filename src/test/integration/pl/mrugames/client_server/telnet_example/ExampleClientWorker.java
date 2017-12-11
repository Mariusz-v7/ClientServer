package pl.mrugames.client_server.telnet_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.CommV2;

public class ExampleClientWorker implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ExampleClientWorker.class);

    private final String name;
    private final CommV2<String, String, String, String> comm;
    private final Runnable onShutdownCommand;

    private volatile Thread thisThread;

    public ExampleClientWorker(CommV2<String, String, String, String> comm, Runnable onShutdownCommand, ClientInfo clientInfo) {
        this.comm = comm;
        this.onShutdownCommand = onShutdownCommand;
        this.name = clientInfo.getName();
    }

    @Override
    public void run() {
        try {

            logger.info("[{}] Client worker has started", name);

            comm.send("Hello! Possible commands: exit, shutdown\n");

            while (!Thread.currentThread().isInterrupted()) {
                String received;
                try {
                    received = comm.receive();
                } catch (InterruptedException e) {
                    logger.info("[{}] Client worker has been interrupted", name);
                    break;
                }

                if (received != null) {
                    logger.info("[{}] Received message: {}", name, received);
                    comm.send("Thank you! Your message was: " + received + "\n");

                    if (received.equals("exit")) {
                        comm.send("Good Bye!\n");
                        break;
                    } else if (received.equals("shutdown")) {
                        comm.send("Shutdown procedure initiated!\n");
                        onShutdownCommand.run();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Client worker has been terminated");
    }
}
