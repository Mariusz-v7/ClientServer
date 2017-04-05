package pl.mrugames.commons.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.mrugames.commons.client.filters.FilterProcessor;
import pl.mrugames.commons.client.io.ClientReader;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClientReaderThreadSpec {
    private ClientReaderThread readerThread;
    private BlockingQueue<String> queue;
    private ClientReader<String> clientReader;
    private CountDownLatch latch;
    private ExecutorService executor;
    private String frame = "123";
    private FilterProcessor filterProcessor;

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws Exception {
        latch = new CountDownLatch(1);

        queue = spy(new LinkedBlockingQueue<>());

        clientReader = mock(ClientReader.class);

        doAnswer(a -> {
            latch.countDown();
            return frame;
        }).when(clientReader).next();


        filterProcessor = mock(FilterProcessor.class);
        readerThread = spy(new ClientReaderThread<>("Reader", queue, clientReader, Collections.emptyList(), filterProcessor));

        executor = Executors.newSingleThreadExecutor();

        doAnswer(a -> Optional.ofNullable(a.getArguments()[0]))
                .when(filterProcessor)
                .filter(any(), any());
    }

    @After
    public void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void givenStreamReturnsFrame_whenRun_thenFrameIsAddedToQueue() throws InterruptedException {
        executor.submit(readerThread);
        latch.await();
        assertThat(queue).contains(frame);
    }

    @Test(timeout = 1000)
    public void whenInterrupt_thenLoopShouldExit() {
        readerThread.interrupt();
        readerThread.run();
    }

    @Test(timeout = 1000)
    public void givenThreadNotStarted_whenJoin_thenExitImmediately() throws InterruptedException {
        readerThread.join();
    }

    @Test(timeout = 1000)
    public void whenJoin_thenAwaitToExitFromRun() throws InterruptedException {
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
