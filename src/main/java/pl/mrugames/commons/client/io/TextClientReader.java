package pl.mrugames.commons.client.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextClientReader implements ClientReader<String> {
    private final BufferedReader bufferedReader;

    public TextClientReader(InputStream inputStream) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public String next() throws Exception {
        return bufferedReader.readLine();
    }

}
