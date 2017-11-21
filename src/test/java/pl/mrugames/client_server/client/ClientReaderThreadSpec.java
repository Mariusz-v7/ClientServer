package pl.mrugames.client_server.client;

import com.codahale.metrics.Counter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClientReaderThreadSpec {
    private ClientReaderThread readerThread;
    private BlockingQueue<String> queue;
    private ClientReader<String> clientReader;
    private CountDownLatch latch;
    private ExecutorService executor;
    private String frame = "123";
    private FilterProcessor filterProcessor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws Exception {
        latch = new CountDownLatch(1);

        queue = spy(new LinkedBlockingQueue<>());

        clientReader = mock(ClientReader.class);

        doAnswer(a -> {
            latch.countDown();
            return frame;
        }).when(clientReader).next();


        filterProcessor = mock(FilterProcessor.class);
        readerThread = spy(new ClientReaderThread<>("Reader", queue, clientReader, Collections.emptyList(), filterProcessor, mock(Counter.class)));

        executor = Executors.newSingleThreadExecutor();

        doAnswer(a -> Optional.ofNullable(a.getArguments()[0]))
                .when(filterProcessor)
                .filter(any(), any());
    }

    @AfterEach
    void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void givenStreamReturnsFrame_whenRun_thenFrameIsAddedToQueue() throws InterruptedException {
        executor.submit(readerThread);
        latch.await();
        assertThat(queue).contains(frame);
    }

    @Test
    void whenInterrupt_thenLoopShouldExit() {
        readerThread.interrupt();
        readerThread.run();
    }

    @Test
    void givenThreadNotStarted_whenJoin_thenExitImmediately() throws InterruptedException {
        readerThread.join();
    }

    @Test
    void whenJoin_thenAwaitToExitFromRun() throws InterruptedException {
        CountDownLatch runSignal = new CountDownLatch(1);
        doAnswer(a -> {
            runSignal.countDown();
            return a.callRealMethod();
        }).when(readerThread).run();

        executor.execute(readerThread);
        runSignal.await();

        readerThread.interrupt();
        readerThread.join();
    }

}
