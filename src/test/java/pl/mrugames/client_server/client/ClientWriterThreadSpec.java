package pl.mrugames.client_server.client;

import com.codahale.metrics.Counter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClientWriterThreadSpec {
    private ClientWriterThread clientWriterThread;
    private OutputStream finalStream;
    private BlockingQueue<String> queue;
    private ClientWriter<String> clientWriter;
    private CountDownLatch latch;
    private ExecutorService executor;
    private String frame = "123";
    private FilterProcessor filterProcessor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws Exception {
        latch = new CountDownLatch(1);

        finalStream = mock(OutputStream.class);
        queue = spy(new LinkedBlockingQueue<>());

        clientWriter = mock(ClientWriter.class);

        doAnswer(a -> {
            latch.countDown();
            queue.poll();
            return null;
        }).when(clientWriter).next(any());

        filterProcessor = mock(FilterProcessor.class);

        clientWriterThread = spy(new ClientWriterThread("Writer", queue, clientWriter,
                10, TimeUnit.MILLISECONDS,
                Collections.emptyList(), filterProcessor,
                mock(Counter.class)));

        executor = Executors.newSingleThreadExecutor();

        queue.put(frame);

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
    void givenQueueIsNotEmpty_whenRun_thenNextIsCalledWithFinalStream() throws Exception {
        executor.execute(clientWriterThread);
        latch.await();
        verify(clientWriter).next(frame);
    }

    @Test
    void givenQueueIsEmpty_whenRun_thenTimeoutException() {
        queue.poll();

        IOExceptionWrapper wrapper = assertThrows(IOExceptionWrapper.class, () -> clientWriterThread.run());
        assertThat(wrapper.getCause()).isInstanceOf(TimeoutException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenInterrupt_thenLoopShouldExit() {
        clientWriterThread = new ClientWriterThread("Writer", queue, clientWriter,
                1000, TimeUnit.MILLISECONDS,
                Collections.emptyList(), filterProcessor,
                mock(Counter.class));

        clientWriterThread.interrupt();
        clientWriterThread.run();
    }

    @Test
    void givenThreadNotStarted_whenJoin_thenExitImmediately() throws InterruptedException {
        clientWriterThread.join();
    }

    @Test
    void whenJoin_thenAwaitToExitFromRun() throws InterruptedException {
        CountDownLatch runSignal = new CountDownLatch(1);
        doAnswer(a -> {
            runSignal.countDown();
            return a.callRealMethod();
        }).when(clientWriterThread).run();

        executor.execute(clientWriterThread);
        runSignal.await();

        clientWriterThread.interrupt();
        clientWriterThread.join();
    }

}
