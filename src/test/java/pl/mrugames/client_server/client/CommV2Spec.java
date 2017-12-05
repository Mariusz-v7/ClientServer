package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.filters.FilterProcessorV2;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommV2Spec {
    private CommV2<String, String, String, String> comm;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessorV2 inputFilterProcessor;
    private FilterProcessorV2 outputFilterProcessor;
    private Instant commCreation;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);
        inputFilterProcessor = mock(FilterProcessorV2.class);
        outputFilterProcessor = mock(FilterProcessorV2.class);

        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(inputFilterProcessor).filter(anyString());
        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(outputFilterProcessor).filter(anyString());

        commCreation = Instant.now();
        comm = new CommV2<>(clientWriter, clientReader, inputFilterProcessor, outputFilterProcessor);
    }

    @Test
    void whenInit_thenLastDataSentAndReceivedIsSetToNow() {
        assertThat(comm.getLastDataReceived()).isBetween(commCreation, Instant.now());
        assertThat(comm.getLastDataSent()).isBetween(commCreation, Instant.now());
    }

    @Test
    void whenReceive_thenUpdateLastDataReceived() throws Exception {
        doReturn("next").when(clientReader).next();

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

        verify(clientWriter).next("testfiltered");
    }

    @Test
    void whenReceive_thenFilterFrame() throws Exception {
        doReturn("next").when(clientReader).next();

        String result = comm.receive();

        verify(inputFilterProcessor).filter("next");

        assertThat(result).isEqualTo("nextfiltered");
    }

    @Test
    void givenTwoFramesFilteredOut_whenRead_thenReturnThirdFrame() throws Exception {
        doReturn("next1", "next2", "next3").when(clientReader).next();

        AtomicLong counter = new AtomicLong();

        doAnswer(a -> {
            if (counter.getAndIncrement() == 2) {
                return Optional.of(a.getArguments()[0] + "filtered");
            } else {
                return Optional.empty();
            }
        }).when(inputFilterProcessor).filter(anyString());

        String result = comm.receive();

        assertThat(result).isEqualTo("next3filtered");
    }

    @Test
    void givenThreadInterrupted_whenReceive_thenInterruptedException() {
        Thread.currentThread().interrupt();

        InterruptedException e = assertThrows(InterruptedException.class, () -> comm.receive());
        assertThat(e.getMessage()).isEqualTo("Thread interrupted before receiving message!");
    }
}
