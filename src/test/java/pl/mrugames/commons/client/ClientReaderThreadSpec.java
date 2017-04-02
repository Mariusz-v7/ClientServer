package pl.mrugames.commons.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.mrugames.commons.client.io.ClientReader;

import java.io.InputStream;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClientReaderThreadSpec {
    private ClientReaderThread<String, InputStream> readerThread;
    private InputStream originalStream;
    private InputStream finalStream;
    private BlockingQueue<String> queue;
    private ClientReader<String, InputStream> clientReader;
    private CountDownLatch latch;
    private ExecutorService executor;
    private String frame = "123";

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws Exception {
        latch = new CountDownLatch(1);

        originalStream = mock(InputStream.class);
        finalStream = mock(InputStream.class);

        queue = spy(new LinkedBlockingQueue<>());

        clientReader = mock(ClientReader.class);

        doReturn(finalStream).when(clientReader).prepare(originalStream);

        doAnswer(a -> {
            latch.countDown();
            return frame;
        }).when(clientReader).next(finalStream);

        readerThread = spy(new ClientReaderThread<>("Reader", originalStream, queue, clientReader));

        executor = Executors.newSingleThreadExecutor();
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
