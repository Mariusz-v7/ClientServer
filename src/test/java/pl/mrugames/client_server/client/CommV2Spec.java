package pl.mrugames.client_server.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.client.io.ClientReader;
import pl.mrugames.client_server.client.io.ClientWriter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class CommV2Spec {
    private CommV2<String, String> comm;
    private ClientWriter<String> clientWriter;
    private ClientReader<String> clientReader;
    private Instant commCreation;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void before() {
        clientWriter = mock(ClientWriter.class);
        clientReader = mock(ClientReader.class);

        commCreation = Instant.now();
        comm = new CommV2<>(clientWriter, clientReader);
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
}
