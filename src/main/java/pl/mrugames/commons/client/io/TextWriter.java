package pl.mrugames.commons.client.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TextWriter implements ClientWriter<String> {
    private final BufferedWriter bufferedWriter;

    public TextWriter(OutputStream outputStream) {
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public void next(String frameToSend) throws Exception {
        bufferedWriter.write(frameToSend);
        bufferedWriter.flush();
    }
}
