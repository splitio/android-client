package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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
        return createSslSocketFactory(null, createTrustManagerFactory(caCertInputStream));
    }

    /**
     * Accepts CA cert(s) InputStream, client PKCS#12 InputStream, and password string.
     */
    @Override
    public SSLSocketFactory create(@Nullable InputStream caCertInputStream, @Nullable InputStream clientPkcs12InputStream, @Nullable String password) throws Exception {
        KeyManagerFactory keyManagerFactory = createKeyManagerFactory(clientPkcs12InputStream, password);
        TrustManagerFactory trustManagerFactory = createTrustManagerFactory(caCertInputStream);

        return createSslSocketFactory(keyManagerFactory, trustManagerFactory);
    }

    /**
     * Creates a TrustManagerFactory from an InputStream containing one or more CA certificates.
     */
    @Nullable
    private TrustManagerFactory createTrustManagerFactory(@Nullable InputStream caCertInputStream) throws Exception {
        if (caCertInputStream == null) {
            return null;
        }

        try {
            // Generate Certificate objects from the InputStream
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> caCertificates = certificateFactory.generateCertificates(caCertInputStream);

            // Start with the system's default trust store to include standard CA certificates
            TrustManagerFactory defaultTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTrustManagerFactory.init((KeyStore) null); // Initialize with system default keystore
            
            // Get the default trust store
            KeyStore defaultTrustStore = null;
            for (TrustManager tm : defaultTrustManagerFactory.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    // Create a new keystore and populate it with system CAs
                    defaultTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    defaultTrustStore.load(null, null);
                    
                    X509Certificate[] acceptedIssuers = ((X509TrustManager) tm).getAcceptedIssuers();
                    for (int j = 0; j < acceptedIssuers.length; j++) {
                        defaultTrustStore.setCertificateEntry("systemCA" + j, acceptedIssuers[j]);
                    }
                    break;
                }
            }
            
            // Create combined trust store with both system CAs and custom proxy CAs
            KeyStore combinedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            combinedTrustStore.load(null, null);
            
            // Add system CA certificates if we found them
            if (defaultTrustStore != null) {
                Enumeration<String> aliases = defaultTrustStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate cert = defaultTrustStore.getCertificate(alias);
                    if (cert != null) {
                        combinedTrustStore.setCertificateEntry(alias, cert);
                    }
                }
            }
            
            // Add custom proxy CA certificates
            int i = 0;
            for (Certificate ca : caCertificates) {
                combinedTrustStore.setCertificateEntry("proxyCA" + (i++), ca);
            }

            // Initialize the TrustManagerFactory with the combined trust store
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(combinedTrustStore);

            return trustManagerFactory;
        } finally {
            caCertInputStream.close();
        }
    }

    private KeyManagerFactory createKeyManagerFactory(@Nullable InputStream clientPkcs12InputStream, @Nullable String password) throws Exception {
        if (clientPkcs12InputStream == null) {
            return null;
        }

        try {
            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            char[] passwordCharArray = new char[0];
            if (password != null) {
                passwordCharArray = password.toCharArray();
            }
            clientKeyStore.load(clientPkcs12InputStream, passwordCharArray);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, (password != null) ? passwordCharArray : new char[0]);

            return keyManagerFactory;
        } finally {
            clientPkcs12InputStream.close();
        }
    }

    private SSLSocketFactory createSslSocketFactory(@Nullable KeyManagerFactory keyManagerFactory, @Nullable TrustManagerFactory trustManagerFactory) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
        TrustManager[] trustManagers = trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null;

        sslContext.init(keyManagers, trustManagers, null);

        return sslContext.getSocketFactory();
    }
}
