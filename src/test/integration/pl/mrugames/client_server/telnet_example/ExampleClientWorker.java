package pl.mrugames.client_server.telnet_example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.client.ClientInfo;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.KillMe;

import javax.annotation.Nullable;

public class ExampleClientWorker implements ClientWorker<String, String> {
    private final static Logger logger = LoggerFactory.getLogger(ExampleClientWorker.class);

    private final String name;
    private final Runnable shutdownServer;
    private final KillMe killme;

    ExampleClientWorker(Runnable shutdownServer, ClientInfo clientInfo, KillMe killme) {
        this.shutdownServer = shutdownServer;
        this.name = clientInfo.getName();
        this.killme = killme;
    }

    @Override
    public String onInit() {
        logger.info("[{]] Client initialized", name);
        return "Hello! Possible commands: exit, shutdown";
    }

    @Override
    public String onRequest(String request) {
        logger.info("[{}] Received message: {}", name, request);

        if (request.equals("exit")) {
            killme.pleaseDoIt();
            return "Bye, bye!";
        } else if (request.equals("shutdown")) {
            shutdownServer.run();
            return "Shutdown procedure initiated!";
        }

        return "Your message was: " + request;
    }

    @Nullable
    @Override
    public String onShutdown() {
        logger.info("[{}] Client terminated", name);

        return "You've been terminated!";
    }
}
