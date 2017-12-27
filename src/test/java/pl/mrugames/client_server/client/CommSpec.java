package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommSpec {
    private Comm<String, String, String, String> comm;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessor inputFilterProcessor;
    private FilterProcessor outputFilterProcessor;
    private Instant commCreation;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private SocketChannel channel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws Exception {
        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);
        inputFilterProcessor = mock(FilterProcessor.class);
        outputFilterProcessor = mock(FilterProcessor.class);
        readBuffer = mock(ByteBuffer.class);
        writeBuffer = mock(ByteBuffer.class);
        channel = mock(SocketChannel.class);

        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(inputFilterProcessor).filter(anyString());
        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(outputFilterProcessor).filter(anyString());

        doReturn(true).when(clientReader).isReady();

        commCreation = Instant.now();
        comm = new Comm<>(clientWriter, clientReader, inputFilterProcessor, outputFilterProcessor, readBuffer, writeBuffer, channel, mock(Timer.class), mock(Timer.class));
    }

    @Test
    void whenInit_thenLastDataSentAndReceivedIsSetToNow() {
        assertThat(comm.getLastDataReceived()).isBetween(commCreation, Instant.now());
        assertThat(comm.getLastDataSent()).isBetween(commCreation, Instant.now());
    }

    @Test
    void whenReceive_thenUpdateLastDataReceived() throws Exception {
        doReturn("next").when(clientReader).read();

        Instant now = Instant.now();
        comm.receive();

        assertThat(comm.getLastDataReceived()).isBetween(now, Instant.now());
    }

    @Test
    void whenSend_thenUpdateLastSentDate() throws Exception {
        Instant now = Instant.now();
        comm.send("next");

        assertThat(comm.getLastDataSent()).isBetween(now, Instant.now());
    }

    @Test
    void whenSend_thenFilterFrame() throws Exception {
        comm.send("test");

        verify(outputFilterProcessor).filter("test");

        verify(clientWriter).write("testfiltered");
    }

    @Test
    void whenReceive_thenFilterFrame() throws Exception {
        doReturn("next").when(clientReader).read();

        String result = comm.receive();

        verify(inputFilterProcessor).filter("next");

        assertThat(result).isEqualTo("nextfiltered");
    }

    @Test
    void givenTwoFramesFilteredOut_whenRead_thenReturnNullNullAndThirdFrame() throws Exception {
        doReturn("next1", "next2", "next3").when(clientReader).read();

        AtomicLong counter = new AtomicLong();

        doAnswer(a -> {
            if (counter.getAndIncrement() == 2) {
                return Optional.of(a.getArguments()[0] + "filtered");
            } else {
                return Optional.empty();
            }
        }).when(inputFilterProcessor).filter(anyString());

        assertThat(comm.receive()).isNull();
        assertThat(comm.receive()).isNull();
        assertThat(comm.receive()).isEqualTo("next3filtered");
    }

    @Test
    void givenReaderIsNotReady_whenRead_thenReturnNull() throws Exception {
        doReturn(false).when(clientReader).isReady();

        assertThat(comm.receive()).isNull();

        verify(clientReader, never()).read();
    }

    @Test
    void whenRead_thenPrepareBuffer() throws Exception {
        comm.receive();
        InOrder inOrder = inOrder(readBuffer, channel, clientReader);

        inOrder.verify(readBuffer).compact();
        inOrder.verify(channel).read(readBuffer);
        inOrder.verify(readBuffer).flip();
        inOrder.verify(clientReader).read();
    }

    @Test
    void givenChannelThrowsException_whenReceive_thenCallFlipInFinallyBlock() throws Exception {
        doThrow(RuntimeException.class).when(channel).read(readBuffer);

        assertThrows(RuntimeException.class, comm::receive);

        verify(readBuffer).flip();
    }


}
