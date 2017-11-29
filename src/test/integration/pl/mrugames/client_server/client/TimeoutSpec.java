package pl.mrugames.client_server.client;

import com.codahale.metrics.MetricRegistry;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.HealthCheckManager;
import pl.mrugames.client_server.client.io.TextReader;
import pl.mrugames.client_server.client.io.TextWriter;
import pl.mrugames.client_server.host.HostManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

class TimeoutSpec {
    private int port = 10000;
    private int timeoutSeconds = 10;
    private SimpleClientWorker clientWorker;
    private CountDownLatch clientConnected;
    private Runnable onClientWorkerCreate;
    private HostManager hostManager;

    @BeforeEach
    void before() throws InterruptedException, IOException {
        hostManager = new HostManager();

        HealthCheckManager.setMetricRegistry(new MetricRegistry());
        clientConnected = new CountDownLatch(1);

        SimpleClientWorker.Factory workerFactory = spy(new SimpleClientWorker.Factory());

        ClientFactory clientFactory = new ClientFactory<>(
                "Timeout Client",
                timeoutSeconds,
                TextWriter::new,
                TextReader::new,
                workerFactory,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        doAnswer(a -> {
            clientWorker = spy((SimpleClientWorker) a.callRealMethod());
            if (onClientWorkerCreate != null)
                onClientWorkerCreate.run();
            clientConnected.countDown();
            return clientWorker;
        }).when(workerFactory).create(any(), any(), any());


        hostManager.newHost("Timeout tests", port, clientFactory);
    }

    @AfterEach
    void after() throws InterruptedException, IOException {
        hostManager.shutdown();
    }

    private void mockHostToSendEverySecond() {
        onClientWorkerCreate = () -> doAnswer(a -> {
            while (!Thread.currentThread().isInterrupted()) {
                clientWorker.getComm().send("test");
                TimeUnit.SECONDS.sleep(1);
            }

            return null;
        }).when(clientWorker).run();
    }

    private void mockClientToSendEverySecond(Socket socket) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                while (!Thread.currentThread().isInterrupted()) {
                    bos.write("test");
                    bos.flush();
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }

    @Test
    void whenNewClientConnects_thenClientWorkerIsNotNull() throws IOException, InterruptedException {
        Socket socket = new Socket("localhost", port);
        clientConnected.await();

        assertThat(clientWorker).isNotNull();
        socket.close();
    }

    @Test
    void givenNewClientConnects_whenNoReadNoWriteForGivenTime_thenClientIsShutDown() throws IOException, InterruptedException {
        Socket socket = new Socket("localhost", port);
        clientConnected.await();

        LocalTime before = LocalTime.now();
        clientWorker.waitForShutdown();
        LocalTime after = LocalTime.now();

        long diff = ChronoUnit.MILLIS.between(before, after);

        assertThat(diff).isCloseTo(timeoutSeconds * 1000, Percentage.withPercentage(95));
        socket.close();
    }

    @Test
    void givenNewClientConnectsAndWritesEverySecond_whenNoResponseForGivenTime_thenClientIsShutDown() throws IOException, InterruptedException {
        Socket socket = new Socket("localhost", port);
        clientConnected.await();

        mockClientToSendEverySecond(socket);

        LocalTime before = LocalTime.now();
        clientWorker.waitForShutdown();
        LocalTime after = LocalTime.now();

        long diff = ChronoUnit.MILLIS.between(before, after);

        assertThat(diff).isCloseTo(timeoutSeconds * 1000, Percentage.withPercentage(95));
        socket.close();
    }

    @Test
    void givenNewClientConnectsAndDoesNotWriteAnythingAndHostSendsEveryOneSecond_whenTimeoutElapses_thenClientIsShutDown() throws InterruptedException, IOException {
        mockHostToSendEverySecond();

        Socket socket = new Socket("localhost", port);
        clientConnected.await();

        LocalTime before = LocalTime.now();
        clientWorker.waitForShutdown();
        LocalTime after = LocalTime.now();

        long diff = ChronoUnit.MILLIS.between(before, after);

        assertThat(diff).isCloseTo(timeoutSeconds * 1000, Percentage.withPercentage(95));
        socket.close();
    }

    @Test
    void givenNewClientConnects_whenBothClientAndHostKeepCommunicating_thenNoTimeout() throws IOException, InterruptedException {
        mockHostToSendEverySecond();

        Socket socket = new Socket("localhost", port);
        clientConnected.await();

        mockClientToSendEverySecond(socket);

        TimeUnit.SECONDS.sleep(timeoutSeconds + 2);

        assertThat(clientWorker.isShutdown()).isFalse();
        assertThat(socket.isClosed()).isFalse();

        socket.close();
    }
}
