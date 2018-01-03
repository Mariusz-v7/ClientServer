package pl.mrugames.client_server.client.io;

import java.io.Serializable;

public interface ClientWriter<FrameType extends Serializable> {
    void write(FrameType frameToSend) throws Exception;
}
