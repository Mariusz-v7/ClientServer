package pl.mrugames.commons.client.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class WebSocketReader implements ClientReader<String> {
    private final BufferedReader bufferedReader;

    public WebSocketReader(InputStream inputStream) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public String next() throws Exception {
        List<String> request = new LinkedList<>();

        do {
            request.add(bufferedReader.readLine());
        } while (bufferedReader.ready());

        return request.stream().collect(Collectors.joining("\r\n"));
    }
}
