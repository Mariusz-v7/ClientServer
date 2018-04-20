package pl.mrugames.nucleus.common.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.mrugames.nucleus.server.SocketHelper;

import java.io.IOException;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

class Frame implements Serializable {
    private final long num;
    private final String str;

    public Frame(long num, String str) {
        this.num = num;
        this.str = str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Frame frame = (Frame) o;

        if (num != frame.num) return false;
        return str != null ? str.equals(frame.str) : frame.str == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (num ^ (num >>> 32));
        result = 31 * result + (str != null ? str.hashCode() : 0);
        return result;
    }
}

class ObjectReaderSpec {
    private SocketHelper socketHelper;
    private ObjectReader<Frame> objectReader;
    private ObjectWriter<Frame> objectWriter;

    @BeforeEach
    void before() throws IOException {
        socketHelper = new SocketHelper();

        objectReader = new ObjectReader<>(socketHelper.getReadBuffer());
        objectWriter = new ObjectWriter<>(socketHelper.getWriteBuffer());
    }

    @AfterEach
    void after() throws Exception {
        socketHelper.close();
    }

    @Test
    void dataExchangeTest() throws Exception {
        objectWriter.write(new Frame(99, "hello!"));
        socketHelper.flush();
        assertThat(objectReader.isReady()).isTrue();

        assertThat(objectReader.read()).isEqualTo(new Frame(99, "hello!"));
    }

}
