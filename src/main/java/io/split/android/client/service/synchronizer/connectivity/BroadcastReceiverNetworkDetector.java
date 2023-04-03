package io.split.android.client.service.synchronizer.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is used to detect network changes using a {@link BroadcastReceiver}.
 */
class BroadcastReceiverNetworkDetector implements NetworkDetector {

    private final Context mContext;
    private final NetworkChangeBroadcastReceiver mNetworkChangeReceiver;
    private final IntentFilter mConnectivityIntentFilter;

    BroadcastReceiverNetworkDetector(Context context, NetworkChangeListener listener) {
        mContext = context;
        mNetworkChangeReceiver = new NetworkChangeBroadcastReceiver(listener);
        mConnectivityIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerNetworkChangeReceiver();
    }

    @Override
    public void activate() {
        mNetworkChangeReceiver.resume();
    }

    @Override
    public void deactivate() {
        mNetworkChangeReceiver.pause();
    }

    @Override
    public void setNetworkChangeListener(NetworkChangeListener listener) {
        mNetworkChangeReceiver.setListener(listener);
    }

    private void registerNetworkChangeReceiver() {
        mContext.registerReceiver(mNetworkChangeReceiver, mConnectivityIntentFilter);
    }

    private void unregisterNetworkChangeReceiver() {
        mContext.unregisterReceiver(mNetworkChangeReceiver);
    }

    private static class NetworkChangeBroadcastReceiver extends BroadcastReceiver {

        private final AtomicBoolean mIsPaused = new AtomicBoolean(false);

        private final AtomicBoolean mIsConnected = new AtomicBoolean(false);

        private NetworkChangeListener mListener;

        NetworkChangeBroadcastReceiver(NetworkChangeListener listener) {
            mListener = listener;
        }

        void setListener(NetworkChangeListener listener) {
            mListener = listener;
        }

        public void pause() {
            mIsPaused.set(true);
        }

        public void resume() {
            mIsPaused.set(false);
            if (mIsConnected.get()) {
                notifyListenerConnected();
            } else {
                notifyListenerDisconnected();
            }
        }

        public boolean isConnected() {
            return mIsConnected.get();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    if (mIsConnected.compareAndSet(false, true)) {
                        notifyListenerConnected();
                    }
                } else {
                    if (mIsConnected.compareAndSet(true, false)) {
                        notifyListenerDisconnected();
                    }
                }
            }
        }

        private void notifyListenerConnected() {
            mListener.onConnected();
        }

        private void notifyListenerDisconnected() {
            mListener.onDisconnected();
        }
    }
}
