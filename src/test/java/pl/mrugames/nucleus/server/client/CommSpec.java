package pl.mrugames.nucleus.server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pl.mrugames.nucleus.common.io.ClientReader;
import pl.mrugames.nucleus.common.io.ClientWriter;
import pl.mrugames.nucleus.server.client.filters.FilterProcessor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommSpec {
    private Comm comm;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessor inputFilterProcessor;
    private FilterProcessor outputFilterProcessor;
    private Instant commCreation;
    private ByteBuffer writeBuffer;
    private SocketChannel channel;
    private Map<String, Protocol<? extends Serializable, ? extends Serializable>> protocols;
    private Lock readBufferLock;
    private Lock writeBufferLock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() throws Exception {
        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);
        inputFilterProcessor = mock(FilterProcessor.class);
        outputFilterProcessor = mock(FilterProcessor.class);
        writeBuffer = mock(ByteBuffer.class);
        channel = mock(SocketChannel.class);

        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(inputFilterProcessor).filter(anyString());
        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(outputFilterProcessor).filter(anyString());

        doReturn(true).when(clientReader).isReady();

        protocols = new HashMap<>();
        protocols.put("default", new Protocol<>(clientWriter, clientReader, inputFilterProcessor, outputFilterProcessor, "default"));
        protocols.put("secondary", new Protocol<>(mock(ClientWriter.class), mock(ClientReader.class), mock(FilterProcessor.class), mock(FilterProcessor.class), "secondary"));

        writeBufferLock = mock(Lock.class);
        readBufferLock = mock(Lock.class);

        commCreation = Instant.now();
        comm = spy(new Comm(protocols, writeBuffer, readBufferLock, writeBufferLock, channel, "default"));

        reset(writeBufferLock);
        reset(readBufferLock);
    }

    @Test
    void givenNoProtocolDefinedForAKey_whenSwitchProtocolToThatKey_thenException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> comm.switchProtocol("does not exist"));

        assertThat(e.getMessage()).isEqualTo("No defined protocol: 'does not exist'");
    }

    @Test
    void whenSwitchProtocol_thenLockBothBuffers() {
        comm.switchProtocol("default");
        verify(readBufferLock).lock();
        verify(writeBufferLock).lock();
        verify(readBufferLock).unlock();
        verify(writeBufferLock).unlock();
    }

    @Test
    void whenInit_thenFirstProtocolIsSet() {
        assertThat(comm.getClientReader()).isSameAs(clientReader);
        assertThat(comm.getClientWriter()).isSameAs(clientWriter);
        assertThat(comm.getInputFilterProcessor()).isSameAs(inputFilterProcessor);
        assertThat(comm.getOutputFilterProcessor()).isSameAs(outputFilterProcessor);
    }

    @Test
    void givenMultipleProtocols_whenSwitchProtocol_thenChangeValues() {
        comm.switchProtocol("secondary");

        assertThat(comm.getClientReader()).isSameAs(protocols.get("secondary").getClientReader());
        assertThat(comm.getClientWriter()).isSameAs(protocols.get("secondary").getClientWriter());
        assertThat(comm.getInputFilterProcessor()).isSameAs(protocols.get("secondary").getInputFilterProcessor());
        assertThat(comm.getOutputFilterProcessor()).isSameAs(protocols.get("secondary").getOutputFilterProcessor());
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

        String result = (String) comm.receive();

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
    void whenSend_thenPrepareBufferAndSend() throws Exception {
        InOrder inOrder = inOrder(writeBuffer, channel, clientWriter);

        comm.send("anything");

        inOrder.verify(clientWriter).write(anyString());
        inOrder.verify(writeBuffer).flip();
        inOrder.verify(channel).write(writeBuffer);
        inOrder.verify(writeBuffer).compact();
    }

    @Test
    void givenWriteThrowsException_whenWrite_thenCallBufferCompactInFinallyBlock() throws IOException {
        doThrow(RuntimeException.class).when(channel).write(writeBuffer);

        assertThrows(RuntimeException.class, () -> comm.send("anything"));

        verify(writeBuffer).compact();
    }

    @Test
    void whenCanRead_thenLockBuffer() throws Exception {
        InOrder inOrder = inOrder(clientReader, readBufferLock);

        comm.canRead();

        inOrder.verify(readBufferLock).lock();
        inOrder.verify(clientReader).isReady();
        inOrder.verify(readBufferLock).unlock();
    }

    @Test
    void givenIsReadyThrowsException_whenCanRead_thenUnlockInFinally() throws Exception {
        doThrow(RuntimeException.class).when(clientReader).isReady();

        assertThrows(RuntimeException.class, comm::canRead);

        verify(readBufferLock).unlock();
    }

    @Test
    void givenCanReadReturnsTrue_whenReceive_thenLockBuffer() throws Exception {
        doReturn(true).when(clientReader).isReady();

        InOrder inOrder = inOrder(comm, readBufferLock, clientReader);

        comm.receive();

        inOrder.verify(readBufferLock).lock();
        inOrder.verify(clientReader).isReady();
        inOrder.verify(clientReader).read();
        inOrder.verify(readBufferLock).unlock();
    }

    @Test
    void givenCanReadReturnsFalse_whenReceive_thenLockBuffer() throws Exception {
        doReturn(false).when(clientReader).isReady();

        InOrder inOrder = inOrder(comm, readBufferLock, clientReader);

        comm.receive();

        inOrder.verify(readBufferLock).lock();
        inOrder.verify(clientReader).isReady();
        inOrder.verify(readBufferLock).unlock();
    }

    @Test
    void givenIsReadyThrowsException_whenReceive_thenUnlockBuffer() throws Exception {
        doThrow(RuntimeException.class).when(clientReader).isReady();

        assertThrows(RuntimeException.class, comm::receive);
        verify(readBufferLock).unlock();
    }

    @Test
    void givenReadThrowsException_whenReceive_thenUnlockBuffer() throws Exception {
        doThrow(RuntimeException.class).when(clientReader).read();

        assertThrows(RuntimeException.class, comm::receive);
        verify(readBufferLock).unlock();
    }

    @Test
    void whenSendThenLockBuffer() throws Exception {
        comm.send("test");

        InOrder inOrder = inOrder(comm, writeBufferLock);
        inOrder.verify(writeBufferLock).lock();
        inOrder.verify(comm).writeToSocket(anyString());
        inOrder.verify(writeBufferLock).unlock();
    }

    @Test
    void givenWriteToSocketThrowsException_whenSend_thenUnlockBuffer() throws Exception {
        doThrow(RuntimeException.class).when(comm).writeToSocket(anyString());
        assertThrows(RuntimeException.class, () -> comm.send("any"));
        verify(writeBufferLock).unlock();

    }

}
