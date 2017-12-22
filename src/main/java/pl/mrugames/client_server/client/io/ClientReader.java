package pl.mrugames.client_server.client.io;

import java.io.Serializable;

public interface ClientReader<FrameType extends Serializable> extends AutoCloseable {
    boolean isReady() throws Exception;

    FrameType read() throws Exception;
}
