package pl.mrugames.client_server.client.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TextWriter implements ClientWriter<String> {
    private final BufferedWriter bufferedWriter;

    public TextWriter(OutputStream outputStream) {
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public void write(String frameToSend) throws Exception {
        bufferedWriter.write(frameToSend);
        bufferedWriter.flush();
    }

    @Override
    public void close() throws Exception {
        bufferedWriter.close();
    }
}
