package pl.mrugames.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.commons.client.ClientWorker;
import pl.mrugames.commons.client.Comm;

import java.util.concurrent.TimeUnit;

public class ExampleClientWorker implements ClientWorker {
    private final static Logger logger = LoggerFactory.getLogger(ExampleClientWorker.class);

    private final String name;
    private final Comm<String, String> comm;
    private final Runnable shutdownSwitch;
    private final Runnable onShutdownCommand;

    private volatile Thread thisThread;

    public ExampleClientWorker(String name, Comm<String, String> comm, Runnable shutdownSwitch, Runnable onShutdownCommand) {
        this.name = name;
        this.comm = comm;
        this.shutdownSwitch = shutdownSwitch;
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public void onClientTermination() {
        logger.info("[{}] Client tread is terminated. Interrupting worker thread.", name);
        thisThread.interrupt();
    }

    @Override
    public void run() {
        thisThread = Thread.currentThread();

        logger.info("[{}] Client worker has started", name);

        comm.send("Hello! Possible commands: exit, shutdown\n");

        while (!Thread.currentThread().isInterrupted()) {
            String received;
            try {
                received = comm.receive(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.info("[{}] Client worker has been interrupted", name);
                break;
            }

            if (received != null) {
                logger.info("[{}] Received message: {}", name, received);
                comm.send("Thank you! Your message was: " + received + "\n");

                if (received.equals("exit")) {
                    comm.send("Good Bye!\n");
                    shutdownSwitch.run();
                } else if (received.equals("shutdown")) {
                    comm.send("Shutdown procedure initiated!\n");
                    onShutdownCommand.run();
                }
            }
        }

        shutdownSwitch.run();

        logger.info("Client worker has been terminated");
    }
}
