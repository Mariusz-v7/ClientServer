package pl.mrugames.client_server;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

public class Metrics {
    private static MetricRegistry registry;
    private static HealthCheckRegistry healthCheckRegistry;

    public static synchronized void setRegistry(MetricRegistry registry) {
        Metrics.registry = registry;
    }

    public static synchronized MetricRegistry getRegistry() {
        if (registry == null) {
            registry = new MetricRegistry();
        }

        return registry;
    }

    public static synchronized void setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
        Metrics.healthCheckRegistry = healthCheckRegistry;
    }

    public static HealthCheckRegistry getHealthCheckRegistry() {
        if (healthCheckRegistry == null) {
            healthCheckRegistry = new HealthCheckRegistry();
        }

        return healthCheckRegistry;
    }
}
