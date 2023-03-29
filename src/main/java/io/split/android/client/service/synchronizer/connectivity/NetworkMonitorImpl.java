package io.split.android.client.service.synchronizer.connectivity;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.utils.logger.Logger;

public class NetworkMonitorImpl implements NetworkMonitor, SplitLifecycleAware, NetworkDetector.NetworkChangeListener {

    private final NetworkDetector mNetworkDetector;

    private final Set<WeakReference<NetworkAware>> mListeners;

    public NetworkMonitorImpl(Context context) {
        mListeners = new HashSet<>();
        mNetworkDetector = NetworkDetectorProvider.getNetworkDetector(context, this);
        mNetworkDetector.setNetworkChangeListener(this);
        mNetworkDetector.activate();
    }

    @Override
    public void pause() {
        Logger.w("NETWORK: Pausing network monitor");
        mNetworkDetector.deactivate();
    }

    @Override
    public void resume() {
        Logger.w("NETWORK: Resuming network monitor");
        mNetworkDetector.activate();
    }

    @Override
    public void register(NetworkAware listener) {
        mListeners.add(new WeakReference<>(listener));
    }

    @Override
    public void unregister() {
        mNetworkDetector.deactivate();
    }

    @Override
    public void onConnected() {
        Logger.w("On connected");

        for (WeakReference<NetworkAware> listener : mListeners) {
            if (listener.get() != null) {
                Logger.w("NETWORK: Notifying listener of network connection: " + listener.get().getClass().getSimpleName());
                listener.get().onNetworkConnected();
            }
        }
    }

    @Override
    public void onDisconnected() {
        for (WeakReference<NetworkAware> listener : mListeners) {
            if (listener.get() != null) {
                Logger.w("NETWORK: Notifying listener of network disconnection: " + listener.get().getClass().getSimpleName());
                listener.get().onNetworkDisconnected();
            }
        }
    }
}
