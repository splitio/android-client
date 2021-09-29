package io.split.android.client.network;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class DevelopmentSslConfig {

    final private SSLSocketFactory sslSocketFactory;
    final private X509TrustManager trustManager;
    final private HostnameVerifier hostnameVerifier;

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
