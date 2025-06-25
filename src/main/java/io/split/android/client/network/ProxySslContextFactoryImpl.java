package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Implementation of ProxySslContextFactory for proxy_cacert and mTLS scenarios.
 */
public class ProxySslContextFactoryImpl implements ProxySslContextFactory {
    /**
     * Create an SSLSocketFactory for proxy connections using a CA certificate from an InputStream.
     * The InputStream will be closed after use.
     */
    @Override
    public SSLSocketFactory create(@Nullable InputStream caCertInputStream) throws Exception {
        TrustManagerFactory tmf = null;
        if (caCertInputStream != null) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            java.util.Collection<? extends Certificate> cas = cf.generateCertificates(caCertInputStream);
            caCertInputStream.close();
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            int i = 0;
            for (Certificate ca : cas) {
                trustStore.setCertificateEntry("proxyCA" + (i++), ca);
            }
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null,
                tmf != null ? tmf.getTrustManagers() : null,
                null);
        return sslContext.getSocketFactory();
    }
}
