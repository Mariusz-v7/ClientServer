package pl.mrugames.commons.client.io;

public interface ClientReader<FrameType> {
    FrameType next() throws Exception;
}
