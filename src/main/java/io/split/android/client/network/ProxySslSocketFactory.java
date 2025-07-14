package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.InputStream;

import javax.net.ssl.SSLSocketFactory;

interface ProxySslSocketFactory {

    /**
     * Create an SSLSocketFactory for proxy connections using a CA certificate from an InputStream.
     * The InputStream will be closed after use.
     *
     * @param caCertInputStream InputStream containing CA certificate (PEM or DER).
     * @return SSLSocketFactory configured for the requested scenario
     */
    SSLSocketFactory create(@Nullable InputStream caCertInputStream) throws Exception;
    
    /**
     * Create an SSLSocketFactory for proxy connections using CA cert and separate client certificate and key files.
     * All InputStreams will be closed after use.
     *
     * @param caCertInputStream InputStream containing one or more CA certificates (PEM or DER).
     * @param clientCertInputStream InputStream containing client certificate (PEM or DER).
     * @param clientKeyInputStream InputStream containing client private key (PEM format, PKCS#8).

     * @return SSLSocketFactory configured for mTLS proxy authentication
     */
    SSLSocketFactory create(@Nullable InputStream caCertInputStream, @Nullable InputStream clientCertInputStream, @Nullable InputStream clientKeyInputStream) throws Exception;
}
