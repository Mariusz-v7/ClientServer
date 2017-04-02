package pl.mrugames.commons.client.io;

public interface ClientWriter<FrameType> {
    void next(FrameType frameToSend) throws Exception;
}
