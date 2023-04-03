package io.split.android.client.service.synchronizer.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.utils.logger.Logger;

/**
 * This class is used to detect network changes using the new {@link android.net.ConnectivityManager.NetworkCallback} API.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class NetworkCallbackNetworkDetector implements NetworkDetector {
    private final Context mContext;
    private ConnectivityManager mConnectivityManager;
    private Callback mNetworkCallback;
    private NetworkRequest mNetworkRequest;

    public NetworkCallbackNetworkDetector(Context context, NetworkChangeListener listener) {
        mContext = context;
        mNetworkCallback = new Callback(listener);
        registerNetworkCallback();
    }

    @Override
    public void activate() {
        mNetworkCallback.resume();
    }

    @Override
    public void deactivate() {
        mNetworkCallback.pause();
    }

    @Override
    public void setNetworkChangeListener(NetworkChangeListener listener) {
        mNetworkCallback = new Callback(listener);
    }

    private void registerNetworkCallback() {
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (mConnectivityManager != null) {
            mConnectivityManager.registerNetworkCallback(getNetworkRequest(), mNetworkCallback);

            // Check if we are already connected to the internet
            if (canReachInternet(mConnectivityManager)) {
                mNetworkCallback.notifyListenerConnected();
            } else {
                mNetworkCallback.notifyListenerDisconnected();
            }
        }
    }

    private NetworkRequest getNetworkRequest() {
        if (mNetworkRequest == null) {
            mNetworkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
        }

        return mNetworkRequest;
    }

    private boolean canReachInternet(ConnectivityManager connectivityManager) {
        if (connectivityManager != null) {
            Network[] allNetworks = connectivityManager.getAllNetworks();
            if (allNetworks != null) {
                for (Network network : allNetworks) {
                    NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                    if (networkCapabilities != null) {
                        // Check if the network has internet capability and is validated
                        boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                        if (hasInternet) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void unregisterNetworkCallback() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    private static class Callback extends ConnectivityManager.NetworkCallback {

        private final NetworkChangeListener mListener;
        private final AtomicBoolean mIsPaused = new AtomicBoolean(false);

        private final AtomicBoolean mIsConnected = new AtomicBoolean(false);

        public Callback(NetworkChangeListener listener) {
            mListener = listener;
        }

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            if (mIsConnected.compareAndSet(false, true)) {
                Logger.w("NETWORK: Notifying network available");
                notifyListenerConnected();
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            if (mIsConnected.compareAndSet(true, false)) {
                Logger.w("NETWORK: Notifying network lost");
                notifyListenerDisconnected();
            }
        }

        public void pause() {
            Logger.w("NETWORK: Pause network detector");
            mIsPaused.set(true);
        }

        public void resume() {
            Logger.w("NETWORK: Resume network detector");
            mIsPaused.set(false);
            if (mIsConnected.get()) {
                notifyListenerConnected();
            } else {
                notifyListenerDisconnected();
            }
        }

        void notifyListenerDisconnected() {
            if (mListener != null) {
                mListener.onDisconnected();
            }
        }

        void notifyListenerConnected() {
            if (mListener != null) {
                mListener.onConnected();
            }
        }
    }
}
