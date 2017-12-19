package pl.mrugames.client_server.client;

public interface ClientWorker<In, Out> {
    Out onRequest(In request);
}
