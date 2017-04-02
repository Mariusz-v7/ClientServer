package pl.mrugames.commons.client.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TextClientWriter implements ClientWriter<String> {
    private final BufferedWriter bufferedWriter;

    public TextClientWriter(OutputStream outputStream) {
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public void next(String frameToSend) throws Exception {
        bufferedWriter.write(frameToSend);
        bufferedWriter.flush();
    }
}
