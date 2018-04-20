package pl.mrugames.nucleus.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketHandshakeParser {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandshakeParser.class);
    private static WebSocketHandshakeParser instance;
    private final static Pattern pattern = Pattern.compile("Sec-WebSocket-Key: (.*?)\r\n");

    public static synchronized WebSocketHandshakeParser getInstance() {
        if (instance == null) {
            instance = new WebSocketHandshakeParser();
        }

        return instance;
    }

    WebSocketHandshakeParser() {
    }

    public String parse(String request) {
        String key = findKey(request);
        String responseKey = computeResponseKey(key);

        return "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + responseKey + "\r\n" +
                "\r\n";
    }

    public boolean isReady(String request) {
        return request.endsWith("\r\n\r\n");
    }

    String findKey(String request) {
        Matcher matcher = pattern.matcher(request);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new BadHandshakeRequestException();
    }

    String computeResponseKey(String requestKey) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.reset();

            String key = requestKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            messageDigest.update(key.getBytes("UTF-8"));

            return Base64.getEncoder().encodeToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            return e.getMessage();
        }

    }
}
