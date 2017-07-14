package pl.mrugames.client_server.client.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextReader implements ClientReader<String> {
    private final BufferedReader bufferedReader;

    public TextReader(InputStream inputStream) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public String next() throws Exception {
        return bufferedReader.readLine();
    }

}
