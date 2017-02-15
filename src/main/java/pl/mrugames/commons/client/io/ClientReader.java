package pl.mrugames.commons.client.io;

import java.io.InputStream;

public interface ClientReader<FrameType, StreamType> {
    /**
     * @return stream which will be passed to the <b>next()</b> method
     * @throws Exception any exception will be caught and propagated further to stop the client.
     */
    StreamType prepare(InputStream originalInputStream) throws Exception;

    /**
     *
     * @param inputStream stream returned from <b>prepare()</b> method
     * @return value to insert into reception queue
     * @throws Exception any exception will be caught and propagated further to stop the client.
     */
    FrameType next(StreamType inputStream) throws Exception;

}
