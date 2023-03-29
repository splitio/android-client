package io.split.android.client.service.synchronizer.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.lang.ref.WeakReference;

/**
 * This class is used to detect network changes using a {@link BroadcastReceiver}.
 */
class BroadcastReceiverNetworkDetector implements NetworkDetector {

    private final Context mContext;
    private final NetworkChangeBroadcastReceiver mNetworkChangeReceiver;
    private final IntentFilter mConnectivityIntentFilter;

    BroadcastReceiverNetworkDetector(Context context, NetworkChangeListener listener) {
        mContext = context;
        mConnectivityIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mNetworkChangeReceiver = new NetworkChangeBroadcastReceiver(listener);
    }

    @Override
    public void activate() {
        registerNetworkChangeReceiver();
    }

    @Override
    public void deactivate() {
        unregisterNetworkChangeReceiver();
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

        private WeakReference<NetworkChangeListener> mListener;

        NetworkChangeBroadcastReceiver(NetworkChangeListener listener) {
            mListener = new WeakReference<>(listener);
        }

        void setListener(NetworkChangeListener listener) {
            mListener = new WeakReference<>(listener);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    mListener.get().onConnected();
                } else {
                    mListener.get().onDisconnected();
                }
            }
        }
    }
}
