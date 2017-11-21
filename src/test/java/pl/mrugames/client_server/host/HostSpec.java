package pl.mrugames.client_server.host;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.ClientFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

class HostSpec {
    private Host host;
    private ServerSocket serverSocket;
    private ClientFactory clientFactory;
    private Socket socket;
    private CountDownLatch latch;

    @BeforeEach
    void before() throws IOException {
        latch = new CountDownLatch(1);

        clientFactory = mock(ClientFactory.class);

        host = spy(new Host("Test", 12345, clientFactory));
        serverSocket = mock(ServerSocket.class);

        socket = mock(Socket.class);

        doAnswer(a -> {
            latch.countDown();
            return a.callRealMethod();
        }).when(host).next(any());

    }

    @AfterEach
    void after() throws InterruptedException {
        host.interrupt();
        host.join();

        if (host.isAlive()) {
            throw new RuntimeException("Host hadn't been shutdown properly!");
        }
    }

    @Test
    void whenNexMethodThrowsException_thenThreadLoopIsNotBroken() throws IOException, InterruptedException, BrokenBarrierException {
        CountDownLatch latch = new CountDownLatch(3);

        doAnswer(a -> {
            latch.countDown();
            throw new Exception();
        }).when(host).next(any());

        host.start();

        latch.await();
    }

    @Test
    void givenHostIsRunWithRealSocket_whenShutDown_thenHostIsShutdownGracefully() throws IOException, InterruptedException {
        host.start();

        latch.await();

        host.interrupt();
        host.join();
    }

    @Test
    void whenRun_thenSetSocket() throws InterruptedException, IOException {
        host.start();

        latch.await();

        verify(host).setSocket(any(ServerSocket.class));

        host.interrupt();
        host.join();
    }

    @Test
    void whenNext_thenSocketAccept() throws IOException {
        host.next(serverSocket);
        verify(serverSocket).accept();
    }

    @Test
    void whenSocketAccept_thenClientFactoryIsCalled() throws IOException {
        doReturn(socket).when(serverSocket).accept();
        host.next(serverSocket);

        verify(clientFactory).create(socket);
    }

    @Test
    void whenInterrupt_thenSocketClose() throws IOException {
        host.setSocket(serverSocket);
        host.interrupt();
        verify(serverSocket).close();
    }

}
