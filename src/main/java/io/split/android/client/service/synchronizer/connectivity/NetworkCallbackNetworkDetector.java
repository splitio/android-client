package io.split.android.client.service.synchronizer.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;

import io.split.android.client.service.synchronizer.HostReachabilityChecker;
import io.split.android.client.utils.logger.Logger;

/**
 * This class is used to detect network changes using the new {@link android.net.ConnectivityManager.NetworkCallback} API.
 */
public class NetworkCallbackNetworkDetector implements HostReachabilityChecker {
    private final ConnectivityManager mConnectivityManager;

    public NetworkCallbackNetworkDetector(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public boolean isReachable() {
        long start = SystemClock.currentThreadTimeMillis();
        if (mConnectivityManager == null) {
            Logger.w("Couldn't get connectivity manager");
            Logger.d("NetworkCallbackNetworkDetector.isReachable() took " + (SystemClock.currentThreadTimeMillis() - start) + "ms and was true due to null manager");
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = mConnectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities activeNetwork = mConnectivityManager.getNetworkCapabilities(network);

            boolean isConnected = activeNetwork != null && (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            Logger.d("NetworkCallbackNetworkDetector.isReachable() took " + (SystemClock.currentThreadTimeMillis() - start) + "ms and was " + isConnected);
            return isConnected;
        } else {
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
            boolean isConnected = networkInfo != null && networkInfo.isConnected();
            Logger.d("NetworkCallbackNetworkDetector.isReachable() took " + (SystemClock.currentThreadTimeMillis() - start) + "ms and was " + isConnected + " (legacy)");

            return isConnected;
        }
    }
}
