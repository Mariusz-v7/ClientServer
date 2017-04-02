package pl.mrugames.commons.websocket;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketHandshakeParser {

    public String parse(String request) {
        String key = findKey(request);
        String responseKey = computeResponseKey(key);

        return "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + responseKey + "\r\n" +
                "\r\n";
    }

    String findKey(String request) {
        Pattern pattern = Pattern.compile("Sec-WebSocket-Key: (.*?)\r\n");
        Matcher matcher = pattern.matcher(request);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new BadHandshakeRequestException();
    }

    String computeResponseKey(String requestKey) {
        byte[] sha1 = DigestUtils.sha1(requestKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
        return Base64.encodeBase64String(sha1);
    }
}
