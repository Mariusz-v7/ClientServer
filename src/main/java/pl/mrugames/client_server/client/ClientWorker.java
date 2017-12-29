package pl.mrugames.client_server.client;

import javax.annotation.Nullable;

public interface ClientWorker<In, Out> {
    @Nullable
    Out onInit();

    @Nullable
    Out onRequest(In request);

    @Nullable
    Out onShutdown();

}
