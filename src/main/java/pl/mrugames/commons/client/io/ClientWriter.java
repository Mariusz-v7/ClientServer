package pl.mrugames.commons.client.io;

import java.io.OutputStream;

public interface ClientWriter<FrameType, StreamType> {
    /**
     * @return stream which will be passed to the <b>next()</b> method
     * @throws Exception any exception will be caught and propagated further to stop the client.
     */
    StreamType prepare(OutputStream originalOutputStream) throws Exception;

    /**
     *
     * @param outputStream stream returned from <b>prepare()</b> method
     * @return value to insert into reception queue
     * @throws Exception any exception will be caught and propagated further to stop the client.
     */
    void next(StreamType outputStream, FrameType frameToSend) throws Exception;

}
