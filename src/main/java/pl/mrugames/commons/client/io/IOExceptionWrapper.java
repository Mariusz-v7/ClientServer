package pl.mrugames.commons.client.io;

public class IOExceptionWrapper extends RuntimeException {
    public IOExceptionWrapper(Throwable cause) {
        super(cause);
    }
}
