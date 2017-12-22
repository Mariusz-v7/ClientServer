package pl.mrugames.client_server.client;

import com.codahale.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.filters.FilterProcessor;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommSpec {
    private Comm<String, String, String, String> comm;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private FilterProcessor inputFilterProcessor;
    private FilterProcessor outputFilterProcessor;
    private Instant commCreation;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);
        inputFilterProcessor = mock(FilterProcessor.class);
        outputFilterProcessor = mock(FilterProcessor.class);

        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(inputFilterProcessor).filter(anyString());
        doAnswer(a -> Optional.of(a.getArguments()[0] + "filtered")).when(outputFilterProcessor).filter(anyString());

        commCreation = Instant.now();
        comm = new Comm<>(clientWriter, clientReader, inputFilterProcessor, outputFilterProcessor, mock(Timer.class), mock(Timer.class));
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

}
