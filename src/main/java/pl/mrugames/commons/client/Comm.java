package pl.mrugames.commons.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Comm<In, Out> {
    private final BlockingQueue<In> in;
    private final BlockingQueue<Out> out;

    Comm(BlockingQueue<In> in, BlockingQueue<Out> out) {
        this.in = in;
        this.out = out;
    }

    public void send(Out frame) {
        out.add(frame);
    }

    public In receive() {
        return in.poll();
    }

    public In receive(long timeout, TimeUnit unit) throws InterruptedException {
        return in.poll(timeout, unit);
    }
}
