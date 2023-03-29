package io.split.android.client.service.synchronizer.connectivity;

public interface NetworkAware {

    void onNetworkConnected();

    void onNetworkDisconnected();
}
