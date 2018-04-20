package pl.mrugames.nucleus.server.object_server;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Frame implements Serializable {
    private static AtomicInteger counter = new AtomicInteger();

    private final int id;
    private final String message;

    public Frame(String message) {
        this.id = counter.incrementAndGet();
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "id=" + id +
                ", message='" + message + '\'' +
                '}';
    }
}
