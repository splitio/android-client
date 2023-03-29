package io.split.android.client.service.synchronizer.connectivity;

interface NetworkDetector {

    void activate();

    void deactivate();

    void setNetworkChangeListener(NetworkChangeListener listener);

    interface NetworkChangeListener {

        void onConnected();

        void onDisconnected();
    }
}
