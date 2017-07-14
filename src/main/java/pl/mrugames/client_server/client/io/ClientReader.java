package pl.mrugames.client_server.client.io;

import java.io.Serializable;

public interface ClientReader<FrameType extends Serializable> {
    FrameType next() throws Exception;
}
