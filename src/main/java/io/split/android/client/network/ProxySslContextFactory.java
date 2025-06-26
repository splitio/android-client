package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.InputStream;

import javax.net.ssl.SSLSocketFactory;

/**
 * Factory interface for creating SSLSocketFactory instances for proxy connections.
 */
interface ProxySslContextFactory {

    /**
     * Create an SSLSocketFactory for proxy connections using a CA certificate from an InputStream.
     * The InputStream will be closed after use.
     *
     * @param caCertInputStream InputStream containing CA certificate (PEM or DER).
     * @return SSLSocketFactory configured for the requested scenario
     * @throws Exception if there is an error loading certificates or creating the context
     */
    SSLSocketFactory create(@Nullable InputStream caCertInputStream) throws Exception;

    /**
     * Create an SSLSocketFactory for proxy connections using CA cert and client PKCS#12 InputStream.
     * All InputStreams will be closed after use.
     *
     * @param caCertInputStream InputStream containing one or more CA certificates (PEM or DER).
     * @param clientPkcs12InputStream InputStream containing client PKCS#12 (e.g. .p12 or .pfx file).
     * @param password password for the client PKCS#12
     * @return SSLSocketFactory configured for mTLS proxy authentication
     * @throws Exception if there is an error loading certificates/keys or creating the context
     */
    SSLSocketFactory create(@Nullable InputStream caCertInputStream, @Nullable InputStream clientPkcs12InputStream, String password) throws Exception;
}
