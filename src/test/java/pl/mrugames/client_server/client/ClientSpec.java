package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ClientSpec {
    private Client client;
    private Socket socket;
    private CountDownLatch executionLatch;
    private ExecutorService ioExecutor;
    private ClientWriterThread writer;
    private ClientReaderThread reader;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void before() throws InterruptedException {
        executionLatch = new CountDownLatch(1);

        writer = mock(ClientWriterThread.class);
        reader = mock(ClientReaderThread.class);

        ioExecutor = spy(Executors.newFixedThreadPool(2));

        socket = mock(Socket.class);
        client = spy(new Client(ioExecutor, "test", socket, writer, reader));

        doAnswer(a -> {
            executionLatch.countDown();
            return a.callRealMethod();
        }).when(client).init();

    }

    @Test
    void givenReaderThreadStops_whenInit_thenReturnedFutureFinishes() throws ExecutionException, InterruptedException {
        doNothing().when(reader).run();

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(writer).run();

        client.init().get();
    }

    @Test
    void givenWriterThreadStops_whenInit_thenReturnedFutureFinishes() throws ExecutionException, InterruptedException {
        doNothing().when(writer).run();

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(reader).run();

        client.init().get();
    }

    @Test
    void whenInit_thenIOTasksAreSubmittedToTheIOExecutor() throws ExecutionException, InterruptedException {
        client.init().get();
        verify(ioExecutor, times(2)).execute(any());
    }

    @Test
    void whenInit_thenIOThreadsAreRun() throws ExecutionException, InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);

        doAnswer(a -> {
            countDownLatch.countDown();
            return null;
        }).when(reader).run();

        doAnswer(a -> {
            countDownLatch.countDown();
            return null;
        }).when(writer).run();

        client.init().get();

        countDownLatch.await();
    }

    @Test
    void givenReaderThreadThrowsException_whenInit_thenFutureEndsWithException() throws ExecutionException, InterruptedException {
        String msg = "READER EXCEPTION";

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(writer).run();

        doThrow(new RuntimeException(msg)).when(reader).run();

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> client.init().get());
        assertThat(executionException.getCause().getMessage()).isEqualTo(msg);
    }

    @Test
    void givenWriterThreadThrowsException_whenInit_thenFutureEndsWithException() throws ExecutionException, InterruptedException {
        String msg = "WRITER EXCEPTION";

        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(reader).run();

        doThrow(new RuntimeException(msg)).when(writer).run();

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> client.init().get());
        assertThat(executionException.getCause().getMessage()).isEqualTo(msg);
    }

    private void sleepThread(Runnable runnable) {
        doAnswer(a -> {
            TimeUnit.DAYS.sleep(1);
            return null;
        }).when(runnable).run();
    }

    private void exceptionThread(Runnable runnable, Throwable exception) {
        doThrow(exception).when(runnable).run();
    }

    @Test
    void givenWriterThreadThrowsException_whenRun_thenCallOnCompleteWithException() {
        RuntimeException runtimeException = new RuntimeException("SOME EXCEPTION");

        sleepThread(reader);
        exceptionThread(writer, runtimeException);

        client.start().join();

        verify(client).onComplete(isNull(), any());
    }

    @Test
    void givenReaderThreadThrowsException_whenRun_thenCallOnCompleteWithException() {
        RuntimeException runtimeException = new RuntimeException("SOME EXCEPTION");

        sleepThread(writer);
        exceptionThread(reader, runtimeException);

        client.start().join();

        verify(client).onComplete(isNull(), any());
    }

    @Test
    void givenWriterThreadEndsException_whenRun_thenCallOnCompleteWithoutException() {
        sleepThread(reader);
        doNothing().when(writer).run();

        client.start().join();

        verify(client).onComplete(any(), isNull(Throwable.class));
    }

    @Test
    void givenReaderThreadEndsException_whenRun_thenCallOnCompleteWithoutException() {
        sleepThread(writer);
        doNothing().when(reader).run();

        client.start().join();

        verify(client).onComplete(any(), isNull(Throwable.class));
    }

    @Test
    void whenOnCompleteWithException_thenCallClose() {
        client.onComplete(null, new Exception());
        verify(client).close();
    }

    @Test
    void whenOnCompleteWithoutException_thenCallClose() {
        client.onComplete(null, null);
        verify(client).close();
    }

    @Test
    void whenClose_thenInterruptReader() {
        client.close();
        verify(reader).interrupt();
    }

    @Test
    void whenClose_thenInterruptWriter() {
        client.close();
        verify(writer).interrupt();
    }

    @Test
    void whenClose_thenSocketClose() throws IOException {
        client.close();
        verify(socket).close();
    }

    @Test
    void whenClose_thenWriterJoin() throws InterruptedException {
        client.close();
        verify(writer).join();
    }

    @Test
    void whenClose_thenReaderJoin() throws InterruptedException {
        client.close();
        verify(reader).join();
    }

}
