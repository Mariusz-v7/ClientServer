module pl.mrugames.nucleus {
    requires metrics.core;
    requires slf4j.api;
    requires jsr305;
    requires metrics.healthchecks;

    exports pl.mrugames.nucleus.server.host;
    exports pl.mrugames.nucleus.server.client;
}