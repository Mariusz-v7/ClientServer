package pl.mrugames.nucleus.common.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.nucleus.server.SocketHelper;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class TextReaderSpec {
    private SocketHelper socketHelper;
    private TextReader textReader;
    private TextWriter textWriter;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        textReader = new TextReader(socketHelper.getReadBuffer());
        textWriter = new TextWriter(socketHelper.getWriteBuffer());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void shouldReadAnyText() throws Exception {
        textWriter.write("1234567");
        socketHelper.flush();

        assertThat(textReader.isReady()).isTrue();
        assertThat(textReader.read()).isEqualTo("1234567");
    }

    @Test
    void givenNothingSent_whenIsReady_thenFalse() throws Exception {
        assertThat(textReader.isReady()).isFalse();
    }

    @Test
    void givenOneCharacterSent_whenIsReady_thenTrue() throws Exception {
        textWriter.write(" ");
        socketHelper.flush();

        assertThat(textReader.isReady()).isTrue();
    }

    @Test
    void givenOneCharacterSentTwice_whenRead_thenReturnBothCharacters() throws Exception {
        textWriter.write("a");
        socketHelper.flush();
        textWriter.write("b");
        socketHelper.flush();

        assertThat(textReader.read()).isEqualTo("ab");
    }

}
