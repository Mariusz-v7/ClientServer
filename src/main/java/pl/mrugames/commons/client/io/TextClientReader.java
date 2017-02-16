package pl.mrugames.commons.client.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextClientReader implements ClientReader<String, BufferedReader> {
    private static TextClientReader instance;

    public static synchronized TextClientReader getInstance() {
        if (instance == null)
            instance = new TextClientReader();

        return instance;
    }

    TextClientReader() {}

    @Override
    public BufferedReader prepare(InputStream originalInputStream) throws Exception {
        return new BufferedReader(new InputStreamReader(originalInputStream));
    }

    @Override
    public String next(BufferedReader inputStream) throws Exception {
        return inputStream.readLine();
    }

}
