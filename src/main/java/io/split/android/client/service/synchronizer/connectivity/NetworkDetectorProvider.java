package io.split.android.client.service.synchronizer.connectivity;

import android.content.Context;

import io.split.android.client.utils.logger.Logger;

/**
 * Provides a {@link NetworkDetector} based on the Android version.
 */
public class NetworkDetectorProvider {

    public static NetworkDetector getNetworkDetector(Context context, NetworkDetector.NetworkChangeListener listener) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Logger.w("NETWORK: Using NetworkCallback to detect network changes");
            return new NetworkCallbackNetworkDetector(context, listener);
        } else {
            Logger.w("NETWORK: Using BroadcastReceiver to detect network changes");
            return new BroadcastReceiverNetworkDetector(context, listener);
        }
    }
}
