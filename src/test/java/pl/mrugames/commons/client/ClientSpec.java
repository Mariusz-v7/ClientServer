package pl.mrugames.commons.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class ClientSpec {
    private Client client;
    private Socket socket;
    private CountDownLatch latch;
    private ExecutorService executor;
    private ExecutorService ioExecutor;
    private ClientWriter writer;
    private ClientReader reader;

    @Before
    @SuppressWarnings("deprecation")
    public void before() throws InterruptedException {
        latch = new CountDownLatch(1);

        writer = mock(ClientWriter.class);
        reader = mock(ClientReader.class);

        executor = Executors.newSingleThreadExecutor();
        ioExecutor = spy(Executors.newFixedThreadPool(2));

        socket = mock(Socket.class);
        client = spy(new Client("test", socket, ioExecutor, writer, reader));

        doAnswer(a -> {
            latch.countDown();
            return a.callRealMethod();
        }).when(client).init();

    }

    @After
    public void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test(timeout = 1000)
    public void whenInterrupted_thenSockedClosed() throws InterruptedException, IOException {
        executor.execute(client);

        latch.await();

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        verify(socket).close();
    }

    @Test
    public void whenInterrupted_thenIOExecutorShutdownNowAndAwaitTermination() throws InterruptedException {
        executor.execute(client);

        latch.await();

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        verify(ioExecutor).shutdownNow();
        verify(ioExecutor).awaitTermination(anyLong(), any());
    }

    @Test(timeout = 1000)
    public void whenHandleIOThreadException_thenClientThreadShouldBeInterrupted() throws InterruptedException {
        executor.execute(client);

        latch.await();

        Thread clientThread = client.getThisThread();

        client.handleIOThreadException(null, new Exception());

        assertThat(clientThread.isInterrupted()).isTrue();
    }

    @Test
    public void whenInit_thenIOExecutorShutdown() {
        client.init();
        verify(ioExecutor).shutdown();
    }

    @Test
    public void whenInit_thenWriterIsExecuted() {
        client.init();
        verify(ioExecutor).execute(writer);
    }

    @Test
    public void whenInit_thenReaderIsExecuted() {
        client.init();
        verify(ioExecutor).execute(reader);
    }
}
