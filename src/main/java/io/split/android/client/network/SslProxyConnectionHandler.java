package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

/**
 * Interface for handling SSL proxy connections that HttpURLConnection cannot support natively.
 * Provides custom connection establishment for PROXY_CACERT and MTLS proxy scenarios
 * where the proxy itself requires SSL authentication.
 */
interface SslProxyConnectionHandler {

    /**
     * Determines if this handler can handle the given proxy configuration.
     *
     * @param proxy The HttpProxy configuration
     * @return true if this handler supports the proxy authentication type
     */
    boolean canHandle(@NonNull HttpProxy proxy);

    /**
     * Executes an HTTP request through an SSL proxy using custom connection handling.
     * This bypasses HttpURLConnection's built-in proxy mechanism entirely.
     *
     * @param proxy                    The SSL proxy configuration
     * @param targetUrl                The final destination URL
     * @param method                   HTTP method for the request
     * @param headers                  Headers to include in the request
     * @param body                     Request body (null for GET requests)
     * @param sslSocketFactory         SSL socket factory configured for proxy authentication
     * @param proxyCredentialsProvider Credentials provider for proxy authentication
     * @return HttpResponse containing the server's response
     * @throws IOException if the request execution fails
     */
    @NonNull
    HttpResponse executeRequest(
            @NonNull HttpProxy proxy,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body,
            @NonNull SSLSocketFactory sslSocketFactory,
            @Nullable ProxyCredentialsProvider proxyCredentialsProvider) throws IOException;
}
