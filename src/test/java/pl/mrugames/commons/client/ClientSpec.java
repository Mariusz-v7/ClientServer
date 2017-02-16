package pl.mrugames.commons.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class ClientSpec {
    private Client client;
    private Socket socket;
    private CountDownLatch executionLatch;
    private ExecutorService executor;
    private ExecutorService ioExecutor;
    private ClientWriterThread writer;
    private ClientReaderThread reader;
    private Runnable onShutdown;

    @Before
    @SuppressWarnings("deprecation")
    public void before() throws InterruptedException {
        executionLatch = new CountDownLatch(1);

        writer = mock(ClientWriterThread.class);
        reader = mock(ClientReaderThread.class);

        onShutdown = mock(Runnable.class);

        executor = Executors.newSingleThreadExecutor();
        ioExecutor = spy(Executors.newFixedThreadPool(2));

        socket = mock(Socket.class);
        client = spy(new Client("test", socket, ioExecutor, writer, reader, onShutdown));

        doAnswer(a -> {
            executionLatch.countDown();
            return a.callRealMethod();
        }).when(client).init();

    }

    @After
    public void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    private void executeAndAwaitTermination() throws InterruptedException {
        executor.execute(client);

        executionLatch.await();

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test(timeout = 1000)
    public void whenInterrupted_thenSockedClosed() throws InterruptedException, IOException {
        executeAndAwaitTermination();
        verify(socket).close();
    }

    @Test
    public void whenClientIsShutdown_thenIOExecutorShutdownNowAndAwaitTermination() throws InterruptedException {
        executeAndAwaitTermination();

        verify(ioExecutor).shutdownNow();
        verify(ioExecutor).awaitTermination(30L, TimeUnit.SECONDS);
    }

    @Test
    public void whenClientIsShutdown_thenOnShutdownIsInvoked() throws InterruptedException {
        executeAndAwaitTermination();
        verify(onShutdown).run();
    }

    @Test(timeout = 1000)
    public void whenHandleIOThreadException_thenIOExecutorIsShutDownNow() throws InterruptedException {
        client.handleIOThreadException(Thread.currentThread(), new Exception());
        verify(ioExecutor).shutdownNow();
    }

    @Test
    public void whenHandleIOThreadException_thenSocketClose() throws IOException {
        client.handleIOThreadException(Thread.currentThread(), new Exception());
        verify(socket).close();
    }

    private void executeWithException() throws InterruptedException {
        doThrow(Exception.class).when(client).init();
        executor.execute(client);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void givenInitThrowsException_whenRun_thenExecutorShutdownNow() throws InterruptedException {
        executeWithException();
        verify(ioExecutor).shutdownNow();
    }

    @Test
    public void givenInitThrowsException_whenRun_thenSocketClose() throws IOException, InterruptedException {
        executeWithException();
        verify(socket).close();
    }

    @Test
    public void givenInitThrowsException_whenRun_thenAwaitTermination() throws IOException, InterruptedException {
        executeWithException();
        verify(ioExecutor).awaitTermination(30, TimeUnit.SECONDS);
    }


    /////
    /////

    @Test(timeout = 1000)
    public void givenReaderThreadStops_whenInit_thenReturnedFutureFinishes() throws ExecutionException, InterruptedException {
        doNothing().when(reader).run();

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(writer).run();

        client.init().get();
    }

    @Test(timeout = 1000)
    public void givenWriterThreadStops_whenInit_thenReturnedFutureFinishes() throws ExecutionException, InterruptedException {
        doNothing().when(writer).run();

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(reader).run();

        client.init().get();
    }

    @Test
    public void whenInit_thenIOTasksAreSubmittedToTheIOExecutor() throws ExecutionException, InterruptedException {
        client.init().get();
        verify(ioExecutor, times(2)).execute(any());
    }

    @Test
    public void whenInit_thenIOExecutorShutdown() {
        client.init();
        verify(ioExecutor).shutdown();
    }

    @Test
    public void whenRun_thenThreadWaitsForIOThreads() throws ExecutionException, InterruptedException {
        CompletableFuture future = spy(CompletableFuture.completedFuture(null));
        doReturn(future).when(client).init();

        client.run();

        verify(future).get();
    }

    @Test
    public void whenInit_thenIOThreadsAreRun() {
        client.init();

        verify(reader).run();
        verify(writer).run();
    }

}
