package io.split.android.client.service.synchronizer.connectivity;

public interface NetworkMonitor {

    void register(NetworkAware listener);

    void unregister();
}
