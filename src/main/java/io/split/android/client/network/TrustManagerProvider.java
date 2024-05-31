package io.split.android.client.network;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.split.android.client.utils.logger.Logger;

class TrustManagerProvider {

    static X509TrustManager getDefaultTrustManager() {
        TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            Logger.e("Error getting default TrustManager: " + e.getMessage());
            return null;
        }

        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (TrustManager tm : trustManagers) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }

        Logger.w("No X509TrustManager found");

        return null;
    }
}
