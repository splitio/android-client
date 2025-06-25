package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.InputStream;

import javax.net.ssl.SSLSocketFactory;

/**
 * Factory interface for creating SSLSocketFactory instances for proxy connections.
 * Supports scenarios such as:
 * - Custom CA certificate for proxy validation (proxy_cacert)
 * - Mutual TLS (mTLS) with client certificate/key and custom CA
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
}
