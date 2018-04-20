package pl.mrugames.nucleus.common.io;

import java.io.Serializable;

public interface ClientWriter<FrameType extends Serializable> {
    void write(FrameType frameToSend) throws Exception;
}
