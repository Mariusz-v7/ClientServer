package pl.mrugames.commons.client;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import pl.mrugames.commons.client.filters.FilterProcessor;
import pl.mrugames.commons.client.io.ClientWriter;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class ClientWriterThreadSpec {
    private ClientWriterThread clientWriterThread;
    private OutputStream finalStream;
    private BlockingQueue<String> queue;
    private ClientWriter<String> clientWriter;
    private CountDownLatch latch;
    private ExecutorService executor;
    private String frame = "123";
    private FilterProcessor filterProcessor;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws Exception {
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
                Collections.emptyList(), filterProcessor));

        executor = Executors.newSingleThreadExecutor();

        queue.put(frame);

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
    public void givenQueueIsNotEmpty_whenRun_thenNextIsCalledWithFinalStream() throws Exception {
        executor.execute(clientWriterThread);
        latch.await();
        verify(clientWriter).next(frame);
    }

    @Test
    public void givenQueueIsEmpty_whenRun_thenTimeoutException() {
        queue.poll();
        expectedException.expect(IOExceptionWrapper.class);
        expectedException.expectCause(new BaseMatcher<Throwable>() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public boolean matches(Object item) {
                return item instanceof TimeoutException;
            }
        });

        clientWriterThread.run();
    }

    @Test(timeout = 1000)
    @SuppressWarnings("unchecked")
    public void whenInterrupt_thenLoopShouldExit() {
        clientWriterThread = new ClientWriterThread("Writer", queue, clientWriter,
                1000, TimeUnit.MILLISECONDS,
                Collections.emptyList(), filterProcessor);

        clientWriterThread.interrupt();
        clientWriterThread.run();
    }

    @Test(timeout = 1000)
    public void givenThreadNotStarted_whenJoin_thenExitImmediately() throws InterruptedException {
        clientWriterThread.join();
    }

    @Test(timeout = 1000)
    public void whenJoin_thenAwaitToExitFromRun() throws InterruptedException {
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
