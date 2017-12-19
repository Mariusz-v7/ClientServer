package pl.mrugames.client_server.host;

import com.codahale.metrics.Timer;
import pl.mrugames.client_server.Metrics;

import static com.codahale.metrics.MetricRegistry.name;

interface HostManagerMetrics {
    Timer clientAcceptMetric = Metrics.getRegistry().timer(name(HostManagerMetrics.class, "hosts", "accept"));
    Timer clientReadMetric = Metrics.getRegistry().timer(name(HostManagerMetrics.class, "hosts", "read"));
}
