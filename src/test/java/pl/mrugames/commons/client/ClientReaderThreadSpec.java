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

        readerThread = new ClientReaderThread<>(originalStream, queue, clientReader);

        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void givenStreamReturnsFrame_whenRun_thenFrameIsAddedToQueue() {
        executor.submit(readerThread);
        assertThat(queue).contains(frame);
    }
}
