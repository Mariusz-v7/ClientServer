package pl.mrugames.client_server;

import com.codahale.metrics.MetricRegistry;

public class HealthCheckManager {
    private volatile static MetricRegistry metricRegistry;

    public static MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public static void setMetricRegistry(MetricRegistry metricRegistry) {
        HealthCheckManager.metricRegistry = metricRegistry;
    }
}
