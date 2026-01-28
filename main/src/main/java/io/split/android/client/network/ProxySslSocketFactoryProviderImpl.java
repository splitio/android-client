package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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

class ProxySslSocketFactoryProviderImpl implements ProxySslSocketFactoryProvider {

    private final Base64Decoder mBase64Decoder;

    ProxySslSocketFactoryProviderImpl() {
        this(new DefaultBase64Decoder());
    }

    ProxySslSocketFactoryProviderImpl(@NonNull Base64Decoder base64Decoder) {
        mBase64Decoder = checkNotNull(base64Decoder);
    }

    @Override
    public SSLSocketFactory create(@Nullable InputStream caCertInputStream) throws Exception {
        // The TrustManagerFactory is necessary because of the CA cert
        return createSslSocketFactory(null, createTrustManagerFactory(caCertInputStream));
    }

    @Override
    public SSLSocketFactory create(@Nullable InputStream caCertInputStream, @Nullable InputStream clientCertInputStream, @Nullable InputStream clientKeyInputStream) throws Exception {
        // The KeyManagerFactory is necessary because of the client certificate and key files
        KeyManagerFactory keyManagerFactory = createKeyManagerFactory(clientCertInputStream, clientKeyInputStream);

        // The TrustManagerFactory is necessary because of the CA cert
        TrustManagerFactory trustManagerFactory = createTrustManagerFactory(caCertInputStream);

        return createSslSocketFactory(keyManagerFactory, trustManagerFactory);
    }

    @Nullable
    private TrustManagerFactory createTrustManagerFactory(@Nullable InputStream caCertInputStream) throws Exception {
        if (caCertInputStream == null) {
            return null;
        }

        try {
            // Generate Certificate objects from the InputStream
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> caCertificates = certificateFactory.generateCertificates(caCertInputStream);

            KeyStore combinedTrustStore = getCombinedStore(caCertificates);

            // Initialize the TrustManagerFactory with the combined trust store
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(combinedTrustStore);

            return trustManagerFactory;
        } finally {
            caCertInputStream.close();
        }
    }

    /**
     * Create a KeyStore with both system CAs and user provided CAs
     * @param caCertificates User provided CAs
     * @return KeyStore
     */
    @NonNull
    private static KeyStore getCombinedStore(Collection<? extends Certificate> caCertificates) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
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
        return combinedTrustStore;
    }

    /**
     * Creates a KeyManagerFactory from separate client certificate and key files.
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
            // Get cert and key
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(clientCertInputStream);

            PrivateKey privateKey = loadPrivateKeyFromPem(clientKeyInputStream);
            
            // Initialize a KeyStore and add the cert and key
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null); // Initialize empty keystore
            keyStore.setKeyEntry("client", privateKey, new char[0], new Certificate[] { cert });
            
            // Initialize the KeyManagerFactory with the created KeyStore
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
            if (keyContent.contains("BEGIN PRIVATE KEY")) {
                // PKCS#8 format - can be loaded directly
                return loadPkcs8PrivateKey(keyContent);
            } else {
                throw new IllegalArgumentException("Unsupported private key format. Must be PEM encoded PKCS#8 format (BEGIN PRIVATE KEY)");
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
        // Extract the base64 encoded private key using proper PEM parsing
        String privateKeyPEM = extractPemContent(keyContent);
        
        // Decode the Base64 encoded private key
        byte[] encoded = mBase64Decoder.decode(privateKeyPEM);
        
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

    /**
     * Extracts the base64 content from a PKCS#8 String.
     *
     * @param pemContent The full PEM content
     * @return The base64 encoded content without PEM boundaries or whitespace
     * @throws IllegalArgumentException if PEM boundaries are not found or malformed
     */
    private String extractPemContent(String pemContent) throws IllegalArgumentException {
        String beginMarker = "-----BEGIN PRIVATE KEY-----";
        int beginIndex = pemContent.indexOf(beginMarker);
        if (beginIndex == -1) {
            throw new IllegalArgumentException("PEM begin marker not found: " + beginMarker);
        }
        String endMarker = "-----END PRIVATE KEY-----";
        int endIndex = pemContent.indexOf(endMarker, beginIndex + beginMarker.length());
        if (endIndex == -1) {
            throw new IllegalArgumentException("PEM end marker not found: " + endMarker);
        }
        String base64Content = pemContent.substring(beginIndex + beginMarker.length(), endIndex);
        return base64Content.replaceAll("\\s+", "");
    }

    private SSLSocketFactory createSslSocketFactory(@Nullable KeyManagerFactory keyManagerFactory, @Nullable TrustManagerFactory trustManagerFactory) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
        TrustManager[] trustManagers = trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null;

        sslContext.init(keyManagers, trustManagers, null);

        return sslContext.getSocketFactory();
    }
}
