package pl.mrugames.commons.client;

public class ClientInfoHelper {
    public void setClientInfo(ClientInfo value) {
        ClientInfo.setClientInfo(value);
    }

    public static void destroy() {
        ClientInfo.destroy();
    }
}
