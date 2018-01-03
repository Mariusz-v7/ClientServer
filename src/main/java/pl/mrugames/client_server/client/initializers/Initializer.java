package pl.mrugames.client_server.client.initializers;

import pl.mrugames.client_server.client.Comm;

import java.io.Serializable;

public interface Initializer<In, Out, Reader extends Serializable, Writer extends Serializable> {
    Comm<In, Out, Reader, Writer> getComm();

    boolean isCompleted();

    Out proceed(In frame);
}
