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

import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class ClientSpec {
    private Client client;
    private Socket socket;
    private ExecutorService executor;
    private CountDownLatch latch;

    @Before
    public void before() throws InterruptedException {
        latch = new CountDownLatch(1);

        socket = mock(Socket.class);
        client = spy(new Client("test", socket));
        executor = Executors.newSingleThreadExecutor();

        doAnswer(a -> {
            latch.countDown();
            return a.callRealMethod();
        }).when(client).initAndWAit();

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
}
