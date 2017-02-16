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
import pl.mrugames.commons.client.io.ClientWriter;

import java.io.OutputStream;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class ClientWriterThreadSpec {
    private ClientWriterThread clientWriterThread;
    private OutputStream originalStream;
    private OutputStream finalStream;
    private BlockingQueue<String> queue;
    private ClientWriter<String, OutputStream> clientWriter;
    private CountDownLatch latch;
    private ExecutorService executor;
    private String frame = "123";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws Exception {
        latch = new CountDownLatch(1);

        originalStream = mock(OutputStream.class);
        finalStream = mock(OutputStream.class);
        queue = spy(new LinkedBlockingQueue<>());

        clientWriter = mock(ClientWriter.class);
        doReturn(finalStream).when(clientWriter).prepare(originalStream);

        doAnswer(a -> {
            latch.countDown();
            queue.poll();
            return null;
        }).when(clientWriter).next(any(), any());

        clientWriterThread = new ClientWriterThread("Writer", originalStream, queue, clientWriter, 10, TimeUnit.MILLISECONDS);

        executor = Executors.newSingleThreadExecutor();

        queue.put(frame);
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
        verify(clientWriter).next(finalStream, frame);
    }

    @Test
    public void givenQueueIsEmpty_whenRun_thenTimeoutException() {
        queue.poll();
        expectedException.expect(IOExceptionWrapper.class);
        expectedException.expectCause(new BaseMatcher<Throwable>() {
            @Override
            public void describeTo(Description description) {}

            @Override
            public boolean matches(Object item) {
                return item instanceof TimeoutException;
            }
        });

        clientWriterThread.run();
    }
}
