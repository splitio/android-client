package io.split.android.client.network;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.split.android.client.utils.logger.Logger;

public class DevelopmentSslConfig {

    private final SSLSocketFactory sslSocketFactory;
    private final X509TrustManager trustManager;
    private final HostnameVerifier hostnameVerifier;

    public DevelopmentSslConfig(X509TrustManager trustManager, HostnameVerifier hostnameVerifier) {
        SSLSocketFactory socketFactory = null;
        X509TrustManager tm = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            socketFactory = sslContext.getSocketFactory();
            tm = trustManager;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Logger.w("Failed to initialize development SSL config: " + e.getLocalizedMessage());
        }

        this.sslSocketFactory = socketFactory;
        this.trustManager = tm;
        this.hostnameVerifier = hostnameVerifier;
    }

    public DevelopmentSslConfig(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager, HostnameVerifier hostnameVerifier) {
        this.sslSocketFactory = sslSocketFactory;
        this.trustManager = trustManager;
        this.hostnameVerifier = hostnameVerifier;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }
}
