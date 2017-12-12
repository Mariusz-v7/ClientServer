package pl.mrugames.client_server;

import com.codahale.metrics.MetricRegistry;

public class Metrics {
    private static MetricRegistry registry;

    public static void setRegistry(MetricRegistry registry) {
        Metrics.registry = registry;
    }

    public static synchronized MetricRegistry getRegistry() {
        if (registry == null) {
            registry = new MetricRegistry();
        }

        return registry;
    }
}
