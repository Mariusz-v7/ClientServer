package pl.mrugames.client_server.client;

import com.codahale.metrics.MetricFilter;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mrugames.client_server.Metrics;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.TextReader;
import pl.mrugames.client_server.client.io.TextWriter;
import pl.mrugames.client_server.host.HostManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

class TimeoutSpec {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int port = 10000;
    private int timeoutSeconds = 10;
    private volatile SimpleClientWorker clientWorker;
    private CountDownLatch clientConnected;
    private Runnable onClientWorkerCreate;
    private HostManager hostManager;
    private ExecutorService executorService;
    private ExecutorService clientExecutor;

    @BeforeEach
    void before() throws InterruptedException, IOException {
        executorService = Executors.newCachedThreadPool();
        clientExecutor = Executors.newCachedThreadPool();
        hostManager = new HostManager();

        clientConnected = new CountDownLatch(1);

        SimpleClientWorker.Factory workerFactory = spy(new SimpleClientWorker.Factory());

        ClientWatchdog watchdog = new ClientWatchdog("Test Watchdog", timeoutSeconds);
        executorService.execute(watchdog);
        watchdog.awaitStart(10, TimeUnit.SECONDS);

        ClientFactory clientFactory = new ClientFactory<>(
                "Timeout Client",
                "Test Client",
                workerFactory,
                Collections.emptyList(),
                TextWriter::new,
                TextReader::new,
                new FilterProcessor(Collections.emptyList()),
                new FilterProcessor(Collections.emptyList()),
                watchdog,
                1024
        );

        doAnswer(a -> {
            clientWorker = spy((SimpleClientWorker) a.callRealMethod());

            if (onClientWorkerCreate != null) {
                onClientWorkerCreate.run();
            }

            clientConnected.countDown();
            return clientWorker;
        }).when(workerFactory).create(any(), any(), any());

        hostManager.newHost("Timeout tests", port, clientFactory, clientExecutor);

        executorService.execute(hostManager);
    }

    @AfterEach
    void after() throws InterruptedException, IOException {
        hostManager.shutdown();
        executorService.shutdownNow();
        boolean result = executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(result);

        result = clientExecutor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(result);

        Metrics.getRegistry().removeMatching(MetricFilter.ALL);

    }

    private void mockHostToSendEverySecond(boolean shouldKeepReceiving) {
        onClientWorkerCreate = () -> doAnswer(a -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    logger.info("Host sending message");
                    clientWorker.getComm().send("host - test");
                    logger.info("Host message sent");

                    if (shouldKeepReceiving) {
                        logger.info("Host receiving message");
                        clientWorker.getComm().receive();
                        logger.info("Host received message");
                    }

                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                logger.error("", e);
            } finally {
                clientWorker.shutdownSignal.countDown();
            }

            return null;
        }).when(clientWorker).run();
    }

    private void mockClientToSendEverySecond(Socket socket) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                InputStream inputStream = socket.getInputStream();

                BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                while (!Thread.currentThread().isInterrupted()) {
                    int available = inputStream.available();
                    logger.info("Client, bytes to read: {}", available);

                    logger.info("Client sending message");
                    bos.write("Client - test\n\r");
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
        assertTimeout(ofSeconds(15), () -> {

            Socket socket = new Socket("localhost", port);
            clientConnected.await();

            LocalTime before = LocalTime.now();
            clientWorker.waitForShutdown();
            LocalTime after = LocalTime.now();

            long diff = ChronoUnit.MILLIS.between(before, after);

            assertThat(diff).isCloseTo(timeoutSeconds * 1000, Percentage.withPercentage(95));
            socket.close();
        });
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
        mockHostToSendEverySecond(false);

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
        mockHostToSendEverySecond(true);

        Socket socket = new Socket("localhost", port);
        clientConnected.await();

        mockClientToSendEverySecond(socket);

        TimeUnit.SECONDS.sleep(timeoutSeconds + 2);

        assertThat(clientWorker.isShutdown()).isFalse();
        assertThat(socket.isClosed()).isFalse();

        socket.close();
    }
}
