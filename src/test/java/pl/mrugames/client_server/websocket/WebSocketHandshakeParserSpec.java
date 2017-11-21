package pl.mrugames.client_server.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketHandshakeParserSpec {
    private final String key = "dGhlIHNhbXBsZSBub25jZQ==";
    private final String request = "GET /example HTTP/1.1\r\n" +
            "Host: example.com:8000\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: " + key + "\r\n" +
            "Sec-WebSocket-Version: 13";

    private final String expectedResponseKey = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";
    private final String expectedResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: " + expectedResponseKey + "\r\n\r\n";

    private WebSocketHandshakeParser parser;

    @BeforeEach
    void before() {
        parser = new WebSocketHandshakeParser();
    }

    @Test
    void givenRequest_thenReturnKey() {
        assertThat(parser.findKey(request)).isEqualTo(key);
    }

    @Test
    void givenBadRequest_whenCannotFindKey_thenException() {
        assertThrows(BadHandshakeRequestException.class, () -> parser.findKey("bad request"));
    }

    @Test
    void givenRequestKey_whenComputeResponseKey_thenReturnProperKey() {
        assertThat(parser.computeResponseKey(key)).isEqualTo(expectedResponseKey);
    }

    @Test
    void givenRequest_whenParse_thenReturnResponse() {
        assertThat(parser.parse(request)).isEqualTo(expectedResponse);
    }
}
