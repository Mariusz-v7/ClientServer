package pl.mrugames.client_server.host.tasks;

import com.codahale.metrics.Timer;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.ClientWorker;
import pl.mrugames.client_server.client.Comm;

import java.io.Serializable;
import java.util.concurrent.Callable;

import static com.codahale.metrics.MetricRegistry.name;

public class ClientRequestTask implements Callable<Void> {
    //    private final List<Initializer> initializers; // TODO: initializers
    private final Comm<Object, Object, Serializable, Serializable> comm;
    private final ClientWorker<Object, Object> clientWorker;
    private final Timer requestProcessingMetric;

    public ClientRequestTask(String name, Comm<Object, Object, Serializable, Serializable> comm, ClientWorker<Object, Object> clientWorker) {
        this.comm = comm;
        this.clientWorker = clientWorker;
        requestProcessingMetric = Metrics.getRegistry().timer(name(ClientRequestTask.class, name));
    }

    @Override
    public Void call() throws Exception {
        try (Timer.Context ignored = requestProcessingMetric.time()) {
            Object request = comm.receive();
            Object response = clientWorker.onRequest(request);
            comm.send(response);
        }

        return null;
    }
}
