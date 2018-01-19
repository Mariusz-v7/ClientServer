package pl.mrugames.client_server.client;

public class ProtocolSwitch {
    private final String name;
    private final SwitchProtocolStrategy switchProtocolStrategy;

    public ProtocolSwitch(String name, SwitchProtocolStrategy switchProtocolStrategy) {
        this.name = name;
        this.switchProtocolStrategy = switchProtocolStrategy;
    }

    public String getName() {
        return name;
    }

    public SwitchProtocolStrategy getSwitchProtocolStrategy() {
        return switchProtocolStrategy;
    }
}
