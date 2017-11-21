package pl.mrugames.client_server.host;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HostManager {
    private final ExecutorService pool;

    public HostManager(int amount) {
        if (amount == 1) {
            pool = Executors.newSingleThreadExecutor();
        } else {
            pool = Executors.newFixedThreadPool(amount);
        }
    }

    public void newHost() {
        
    }
}
