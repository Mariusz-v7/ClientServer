package pl.mrugames.client_server.tasks;

import com.codahale.metrics.Timer;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.Client;
import pl.mrugames.client_server.client.ProtocolSwitch;
import pl.mrugames.client_server.client.SwitchProtocolStrategy;

import java.util.concurrent.Callable;

import static com.codahale.metrics.MetricRegistry.name;

public class RequestExecuteTask implements Callable<Void> {
    private final Client client;
    private final Object frame;

    private final Timer requestProcessingMetric;

    public RequestExecuteTask(Client client, Object frame) {
        this.client = client;
        this.frame = frame;

        requestProcessingMetric = Metrics.getRegistry().timer(name(ClientRequestTask.class, client.getName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void call() throws Exception {

        try (Timer.Context ignored = requestProcessingMetric.time()) {
            Object response = client.getClientWorker().onRequest(frame);
            ProtocolSwitch protocolSwitch = client.getProtocolSwitch();

            if (protocolSwitch != null && protocolSwitch.getSwitchProtocolStrategy() == SwitchProtocolStrategy.BEFORE_RESPONSE_SENT) {
                client.getComm().switchProtocol(protocolSwitch.getName());
                client.scheduleProtocolSwitch(null);
            }

            if (response != null) {
                client.getComm().send(response);
            }

            if (protocolSwitch != null && protocolSwitch.getSwitchProtocolStrategy() == SwitchProtocolStrategy.AFTER_RESPONSE_SENT) {
                client.getComm().switchProtocol(protocolSwitch.getName());
                client.scheduleProtocolSwitch(null);
            }
        } catch (Exception e) {
            client.getTaskExecutor().submit(new ClientShutdownTask(client), client.getRequestTimeoutSeconds());
            throw e;
        }

        return null;
    }

    Object getFrame() {
        return frame;
    }
}
