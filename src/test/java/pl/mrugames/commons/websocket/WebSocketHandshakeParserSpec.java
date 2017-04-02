package pl.mrugames.commons.websocket;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class WebSocketHandshakeParserSpec {
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

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void before() {
        parser = new WebSocketHandshakeParser();
    }

    @Test
    public void givenRequest_thenReturnKey() {
        assertThat(parser.findKey(request)).isEqualTo(key);
    }

    @Test
    public void givenBadRequest_whenCannotFindKey_thenException() {
        expectedException.expect(BadHandshakeRequestException.class);
        parser.findKey("bad request");
    }

    @Test
    public void givenRequestKey_whenComputeResponseKey_thenReturnProperKey() {
        assertThat(parser.computeResponseKey(key)).isEqualTo(expectedResponseKey);
    }

    @Test
    public void givenRequest_whenParse_thenReturnResponse() {
        assertThat(parser.parse(request)).isEqualTo(expectedResponse);
    }
}
