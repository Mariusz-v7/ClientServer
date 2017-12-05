package pl.mrugames.client_server.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Comm<In, Out> {
    private final BlockingQueue<In> in;
    private final BlockingQueue<Out> out;

    Comm(BlockingQueue<In> in, BlockingQueue<Out> out) {
        this.in = in;
        this.out = out;
    }

    public synchronized void send(Out frame) {
        out.add(frame);
    }

    public synchronized In receive() {
        return in.poll();
    }

    public synchronized In receive(long timeout, TimeUnit unit) throws InterruptedException {
        return in.poll(timeout, unit);
    }
}
