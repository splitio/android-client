package io.split.android.client.network;

public interface TlsUpdater {

    /**
     * Return true if the device may need a TLS update.
     */
    boolean couldBeOld();

    /**
     * Perform the TLS update.
     */
    void update();
}
