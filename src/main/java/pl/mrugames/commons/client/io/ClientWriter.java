package pl.mrugames.commons.client.io;

import java.io.Serializable;

public interface ClientWriter<FrameType extends Serializable> {
    void next(FrameType frameToSend) throws Exception;
}
