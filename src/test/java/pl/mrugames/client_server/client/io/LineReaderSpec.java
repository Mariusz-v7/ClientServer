package pl.mrugames.client_server.client.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.client_server.SocketHelper;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class LineReaderSpec {
    private SocketHelper socketHelper;
    private LineReader lineReader;
    private LineWriter lineWriter;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        lineReader = new LineReader(socketHelper.getReadBuffer());
        lineWriter = new LineWriter(socketHelper.getWriteBuffer());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void givenNothingSent_whenIsReady_thenFalse() throws Exception {
        assertThat(lineReader.isReady()).isFalse();
    }

    @Test
    void givenLineSent_whenIsReady_thenTrue() throws Exception {
        lineWriter.write("a line");
        socketHelper.flush();

        assertThat(lineReader.isReady()).isTrue();
    }

    @Test
    void givenFragmentSent_whenIsReady_thenFalse() throws Exception {
        socketHelper.write("without nl".getBytes());
        assertThat(lineReader.isReady()).isFalse();
    }

    @Test
    void givenNlEnding_whenIsReady_thenTrue() throws Exception {
        socketHelper.write(("with nl" + lineWriter.getLineEnding()).getBytes());
        assertThat(lineReader.isReady()).isTrue();
    }

    @Test
    void givenLineSent_whenRead_thenReturnLine() throws Exception {
        lineWriter.write("a line");
        socketHelper.flush();

        assertThat(lineReader.read()).isEqualTo("a line");
    }

    @Test
    void givenTwoLinesSent_whenRead_thenReturnFirstAndThenSecondLine() throws Exception {
        lineWriter.write("line 1");
        lineWriter.write("line 2");
        socketHelper.flush();

        assertThat(lineReader.read()).isEqualTo("line 1");
        assertThat(lineReader.read()).isEqualTo("line 2");
    }

    @Test
    void givenLineAndAHalfIsSent_whenRead_thenReturnOnlyFirstLine() throws Exception {
        socketHelper.write(("first" + lineWriter.getLineEnding() + "and a...").getBytes());

        assertThat(lineReader.read()).isEqualTo("first");
        assertThat(lineReader.isReady()).isFalse();

        socketHelper.write(lineWriter.getLineEnding().getBytes());

        assertThat(lineReader.isReady()).isTrue();
        assertThat(lineReader.read()).isEqualTo("and a...");
    }

}
