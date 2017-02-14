package pl.mrugames.commons.host;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import pl.mrugames.commons.client.ClientFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class HostSpec {
    private Host host;
    private ServerSocket serverSocket;
    private ClientFactory clientFactory;
    private Socket socket;

    @Before
    public void before() throws IOException {
        clientFactory = mock(ClientFactory.class);

        host = spy(new Host("Test", 12345, clientFactory));
        serverSocket = mock(ServerSocket.class);

        socket = mock(Socket.class);
    }

    @After
    public void after() throws InterruptedException {
        host.interrupt();
        host.join();

        if (host.isAlive()) {
            throw new RuntimeException("Host hadn't been shutdown properly!");
        }
    }

    @Test(timeout = 1000)
    public void whenNexMethodThrowsException_thenThreadLoopIsNotBroken() throws IOException, InterruptedException, BrokenBarrierException {
        CountDownLatch latch = new CountDownLatch(3);

        doAnswer(a -> {
            latch.countDown();
            throw new Exception();
        }).when(host).next(any());

        host.start();

        latch.await();
    }

    @Test(timeout = 1000)
    public void givenHostIsRunWithRealSocket_whenShutDown_thenHostIsShutdownGracefully() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(a -> {
            latch.countDown(); // make sure that method got called before interrupt
            return a.callRealMethod();
        }).when(host).next(any());

        host.start();

        latch.await();

        host.interrupt();
        host.join();
    }

    @Test
    public void whenRun_thenSetSocket() throws InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(a -> {
            latch.countDown(); // make sure that method got called before interrupt
            return a.callRealMethod();
        }).when(host).next(any());

        host.start();

        latch.await();

        verify(host).setSocket(any(ServerSocket.class));

        host.interrupt();
        host.join();
    }

    @Test
    public void whenNext_thenSocketAccept() throws IOException {
        host.next(serverSocket);
        verify(serverSocket).accept();
    }

    @Test
    public void whenSocketAccept_thenClientExecutorIsCalled() throws IOException {
        doReturn(socket).when(serverSocket).accept();
        host.next(serverSocket);

        verify(clientFactory).create(socket);
    }

    @Test
    public void whenInterrupt_thenSocketClose() throws IOException {
        host.setSocket(serverSocket);
        host.interrupt();
        verify(serverSocket).close();
    }

}
