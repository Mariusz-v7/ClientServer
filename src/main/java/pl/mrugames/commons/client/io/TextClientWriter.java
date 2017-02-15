package pl.mrugames.commons.client.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TextClientWriter implements ClientWriter<String, BufferedWriter> {

    @Override
    public BufferedWriter prepare(OutputStream originalOutputStream) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(originalOutputStream));
    }

    @Override
    public void next(BufferedWriter outputStream, String frameToSend) throws Exception {
        outputStream.write(frameToSend);
    }
}
