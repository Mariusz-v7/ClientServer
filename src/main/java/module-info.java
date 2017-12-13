module pl.mrugames.client_server {
    requires metrics.core;
    requires slf4j.api;
    requires jsr305;
    requires metrics.healthchecks;

    exports pl.mrugames.client_server.host;
    exports pl.mrugames.client_server.client;
}