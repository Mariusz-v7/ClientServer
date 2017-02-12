package pl.mrugames.commons.host;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class HostSpec {
    private Host host;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    @Before
    public void before() throws IOException {
        host = spy(new Host("Test", 12345));
        serverSocket = mock(ServerSocket.class);
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void after() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        if (!executor.isTerminated()) {
            throw new RuntimeException("Host hadn't been shutdown properly!");
        }
    }

    @Test
    public void whenNexMethodThrowsException_thenThreadLoopIsNotBroken() throws IOException, InterruptedException, BrokenBarrierException {
        CountDownLatch latch = new CountDownLatch(3);

        doAnswer(a -> {
            latch.countDown();
            throw new Exception();
        }).when(host).next(any());

        executor.execute(host);

        latch.await();
    }

    @Test
    public void whenNext_thenSocketAccept() throws IOException {
        host.next(serverSocket);
        verify(serverSocket).accept();
    }

}
