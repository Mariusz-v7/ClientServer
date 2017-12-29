package pl.mrugames.client_server.client;

import com.codahale.metrics.MetricFilter;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.io.LineReader;
import pl.mrugames.client_server.client.io.LineWriter;
import pl.mrugames.client_server.host.HostManager;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class TimeoutSpec {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int port = 10000;
    private final int timeout = 5;
    private HostManager hostManager;
    private TimeoutClientWorker clientWorker;
    private ExecutorService executorService;
    private LineWriter lineWriter;

    @BeforeEach
    void before() throws IOException, InterruptedException {
        executorService = Executors.newCachedThreadPool();

        clientWorker = new TimeoutClientWorker();

        ClientFactory clientFactory = new ClientFactoryBuilder<>(buffer -> {
            lineWriter = new LineWriter(buffer);
            return lineWriter;
        },
                LineReader::new,
                (ClientWorkerFactory<String, String, String, String>) (comm, clientInfo, killme) ->
                        clientWorker,
                executorService)
                .setTimeout(timeout)
                .setName("Text Server")
                .build();

        hostManager = new HostManager();
        hostManager.newHost("Timeout Host", port, clientFactory, executorService);

        executorService.submit(hostManager);
    }

    @AfterEach
    void after() {
        executorService.shutdownNow();
        Metrics.getRegistry().removeMatching(MetricFilter.ALL);
    }

    @Test
    void givenNooneSendsAnything_when5secondsPass_thenDisconnect() throws IOException, InterruptedException {
        try (Socket ignored = new Socket("localhost", port)) {
            assertTimeout(ofSeconds(1), () -> clientWorker.initLatch.await());

            long before = System.nanoTime();
            assertTimeout(ofSeconds(timeout + 1), () -> clientWorker.shutdownLatch.await());
            long after = System.nanoTime();

            assertThat(TimeUnit.NANOSECONDS.toSeconds(after - before)).isGreaterThanOrEqualTo(timeout);
        }
    }

    @Test
    void givenClientSendsMessagesButServerNot_when5SecondsPass_thenDisconnectAnyway() throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            assertTimeout(ofSeconds(1), () -> clientWorker.initLatch.await());

            AtomicInteger sum = new AtomicInteger();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            executorService.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        sum.incrementAndGet();
                        outputStreamWriter.write("hello" + lineWriter.getLineEnding());
                        outputStreamWriter.flush();
                        Thread.sleep(250);
                    } catch (Exception e) {
                        break;
                    }
                }
            });

            long before = System.nanoTime();
            assertTimeout(ofSeconds(timeout + 1), () -> clientWorker.shutdownLatch.await());
            long after = System.nanoTime();

            assertThat(TimeUnit.NANOSECONDS.toSeconds(after - before)).isGreaterThanOrEqualTo(timeout);
            assertThat(sum.get()).isCloseTo(clientWorker.amountReceived, Offset.offset(2));
        }
    }

    //TODO: server keeps sending but client not = timeout
    //TODO: both are sending = no timeout

    //TODO: client side timeouts

}
