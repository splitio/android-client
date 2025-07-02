package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.split.android.client.utils.logger.Logger;

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
     * Accepts CA cert(s) InputStream, client certificate InputStream, and client key InputStream.
     */
    @Override
    public SSLSocketFactory create(@Nullable InputStream caCertInputStream, @Nullable InputStream clientCertInputStream, @Nullable InputStream clientKeyInputStream) throws Exception {
        KeyManagerFactory keyManagerFactory = createKeyManagerFactory(clientCertInputStream, clientKeyInputStream);
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

    /**
     * Creates a KeyManagerFactory from separate certificate and key files.
     * This approach is more compatible with Android than using PKCS#12 files.
     * 
     * @param clientCertInputStream InputStream containing client certificate (PEM or DER)
     * @param clientKeyInputStream InputStream containing client private key (PEM format)
     * @return KeyManagerFactory initialized with the client certificate and key
     * @throws Exception if there is an error loading the certificate or key
     */
    private KeyManagerFactory createKeyManagerFactory(@Nullable InputStream clientCertInputStream, 
                                                    @Nullable InputStream clientKeyInputStream) throws Exception {
        if (clientCertInputStream == null || clientKeyInputStream == null) {
            return null;
        }

        try {
            // 1. Load the certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(clientCertInputStream);
            
            // 2. Load the private key
            PrivateKey privateKey = loadPrivateKeyFromPem(clientKeyInputStream);
            
            // 3. Create a KeyStore and add the certificate and key
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null); // Initialize empty keystore
            keyStore.setKeyEntry("client", privateKey, new char[0], new Certificate[] { cert });
            
            // 4. Initialize KeyManagerFactory with the KeyStore
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, new char[0]);
            
            return keyManagerFactory;
        } finally {
            clientCertInputStream.close();
            clientKeyInputStream.close();
        }
    }

    /**
     * Loads a private key from a PEM-encoded input stream.
     * Only supports PKCS#8 format (BEGIN PRIVATE KEY).
     * 
     * @param keyInputStream InputStream containing the PEM-encoded private key
     * @return PrivateKey object
     * @throws Exception if there is an error loading the key
     */
    private PrivateKey loadPrivateKeyFromPem(InputStream keyInputStream) throws Exception {
        try {
            // Read the key file
            String keyContent = readInputStream(keyInputStream);
            
            // Check if it's PKCS#8 format
            if (keyContent.contains("BEGIN PRIVATE KEY")) {
                // PKCS#8 format - can be loaded directly
                return loadPkcs8PrivateKey(keyContent);
            } else {
                throw new IllegalArgumentException("Unsupported private key format. Must be PEM encoded PKCS#8 format (BEGIN PRIVATE KEY). " +
                    "Use OpenSSL to convert other formats: openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in key.pem -out key_pkcs8.pem");
            }
        } catch (Exception e) {
            Logger.e("Error loading private key: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Loads a PKCS#8 format private key.
     */
    private PrivateKey loadPkcs8PrivateKey(String keyContent) throws Exception {
        // Extract the base64 encoded private key
        String privateKeyPEM = keyContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        
        // Decode the Base64 encoded private key
        byte[] encoded = android.util.Base64.decode(privateKeyPEM, android.util.Base64.DEFAULT);
        
        // Create a PKCS8 key spec and generate the private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try {
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            // Try with EC algorithm if RSA fails
            try {
                keyFactory = KeyFactory.getInstance("EC");
                return keyFactory.generatePrivate(keySpec);
            } catch (InvalidKeySpecException ecException) {
                Logger.e("Error loading private key: Neither RSA nor EC algorithms could load the key");
                throw new IllegalArgumentException("Invalid PKCS#8 private key format. Key could not be loaded with RSA or EC algorithms.", e);
            }
        }
    }
    
    /**
     * Helper method to read an InputStream into a String.
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private SSLSocketFactory createSslSocketFactory(@Nullable KeyManagerFactory keyManagerFactory, @Nullable TrustManagerFactory trustManagerFactory) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
        TrustManager[] trustManagers = trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null;

        sslContext.init(keyManagers, trustManagers, null);

        return sslContext.getSocketFactory();
    }
}
