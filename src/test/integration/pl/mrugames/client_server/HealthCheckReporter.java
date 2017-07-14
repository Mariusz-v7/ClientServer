package pl.mrugames.client_server;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

public class HealthCheckReporter {
    public static Thread createAndStart() {
        HealthCheckManager.setMetricRegistry(new MetricRegistry());

        Thread thread = new Thread() {
            @Override
            public void run() {
                ConsoleReporter reporter = ConsoleReporter.forRegistry(HealthCheckManager.getMetricRegistry())
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .build();
                reporter.start(1, TimeUnit.SECONDS);
            }
        };

        thread.start();
        return thread;
    }
}
