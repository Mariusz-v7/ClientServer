package pl.mrugames.commons.client.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TextClientWriter implements ClientWriter<String, BufferedWriter> {
    private static TextClientWriter instance;

    public static synchronized TextClientWriter getInstance() {
        if (instance == null)
            instance = new TextClientWriter();

        return instance;
    }

    TextClientWriter() {}

    @Override
    public BufferedWriter prepare(OutputStream originalOutputStream) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(originalOutputStream));
    }

    @Override
    public void next(BufferedWriter outputStream, String frameToSend) throws Exception {
        outputStream.write(frameToSend);
        outputStream.flush();
    }
}
