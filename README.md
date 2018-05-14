# NUCLEUS

A multithreaded TCP Socket server.

- supports WebSocket
- possibility to define own protocols
- requests queueing
- thread per request
- uses non-blocking sockets

# WebSocket Example

To handle requests from your clients you need to define the logic in a Worker:

```
public class Worker implements ClientWorker {
    private final Comm comm;
    private final Runnable onClientShutDown;

    public Worker(Comm comm, Runnable onClientShutDown) {
        this.comm = comm;
        this.onClientShutDown = onClientShutDown;
    }

    @Override
    public Object onInit() {
        return null;
    }

    @Override
    public Object onRequest(Object request) {
        return request;  // return back the request, you define the logic here
    }

    @Override
    public Object onShutdown() {
        return null;
    }
}
```

You have to provide a Worker Factory:

```
public class WebSocketWorkerFactory implements ClientWorkerFactory<String, String> {
    private final Runnable onShutdownCommand;

    public WebSocketWorkerFactory(Runnable onShutdownCommand) {
        this.onShutdownCommand = onShutdownCommand;
    }

    @Override
    public ClientWorker create(Comm comm, ClientInfo clientInfo, ClientController controller) {
        return new Worker(comm, onShutdownCommand);
    }
}
```

Then you can put it all together and create Host Manager, which will start your server:

```
int numThreads = 4;
HostManager hostManager = HostManager.create(numThreads);

int connectionTimeoutSeconds = 60;
int requestTimeoutSeconds = 30;
int bufferSize = 1024;

ClientFactory<String, String> clientFactory = ClientFactories.createClientFactoryForWSServer(
        "WebSocket Host",
        connectionTimeoutSeconds,
        requestTimeoutSeconds,
        new WebSocketWorkerFactory(() -> hostManager.shutdown()),
        bufferSize
);

hostManager.newHost("Main Host", port, clientFactory);
hostManager.run();
```

You can use predefined methods in ClientFactories which helps to create different types of servers (like in above example we use WebSocket Server).