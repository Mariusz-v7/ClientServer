package pl.mrugames.commons.client;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
    private ExecutorService ioExecutor;
    private ClientWriterThread writer;
    private ClientReaderThread reader;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    @SuppressWarnings("deprecation")
    public void before() throws InterruptedException {
        executionLatch = new CountDownLatch(1);

        writer = mock(ClientWriterThread.class);
        reader = mock(ClientReaderThread.class);

        ioExecutor = spy(Executors.newFixedThreadPool(2));

        socket = mock(Socket.class);
        client = spy(new Client("test", socket, ioExecutor, writer, reader));

        doAnswer(a -> {
            executionLatch.countDown();
            return a.callRealMethod();
        }).when(client).init();

    }
    @Test(timeout = 1000)
    public void givenInitReturnsCompletedFuture_whenRun_thenCloseSocket() throws InterruptedException, IOException {
        doReturn(CompletableFuture.completedFuture(null)).when(client).init();
        client.run();

        verify(socket).close();
    }

    @Test
    public void givenInitReturnsCompletedFuture_whenRun_thenIOExecutorShutdownNowAndAwaitTermination() throws InterruptedException {
        doReturn(CompletableFuture.completedFuture(null)).when(client).init();
        client.run();

        verify(ioExecutor).shutdownNow();
        verify(ioExecutor).awaitTermination(30L, TimeUnit.SECONDS);
    }

    @Test
    public void givenInitThrowsException_whenRun_thenExecutorShutdownNow() throws InterruptedException {
        doThrow(new RuntimeException()).when(client).init();
        client.run();

        verify(ioExecutor).shutdownNow();
    }

    @Test
    public void givenInitThrowsException_whenRun_thenSocketClose() throws IOException, InterruptedException {
        doThrow(new RuntimeException()).when(client).init();
        client.run();

        verify(socket).close();
    }

    @Test
    public void givenInitThrowsException_whenRun_thenAwaitTermination() throws IOException, InterruptedException {
        doThrow(new RuntimeException()).when(client).init();
        client.run();

        verify(ioExecutor).awaitTermination(30, TimeUnit.SECONDS);
    }

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
    public void whenInit_thenIOThreadsAreRun() throws ExecutionException, InterruptedException {
        client.init().get();

        verify(reader).run();
        verify(writer).run();
    }

    private BaseMatcher<Throwable> getBaseMatcherForNestedException(String exceptionMessage) {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof RuntimeException && ((RuntimeException) item).getMessage().equals(exceptionMessage);
            }

            @Override
            public void describeTo(Description description) {}
        };
    }

    @Test
    public void givenReaderThreadThrowsException_whenInit_thenFutureEndsWithException() throws ExecutionException, InterruptedException {
        String msg = "READER EXCEPTION";

        expectedException.expect(ExecutionException.class);
        expectedException.expectCause(getBaseMatcherForNestedException(msg));

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(writer).run();

        doThrow(new RuntimeException(msg)).when(reader).run();
        client.init().get();
    }

    @Test
    public void givenWriterThreadThrowsException_whenInit_thenFutureEndsWithException() throws ExecutionException, InterruptedException {
        String msg = "WRITER EXCEPTION";

        expectedException.expect(ExecutionException.class);
        expectedException.expectCause(getBaseMatcherForNestedException(msg));

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(reader).run();

        doThrow(new RuntimeException(msg)).when(writer).run();
        client.init().get();
    }

}
