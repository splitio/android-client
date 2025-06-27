package io.split.android.client.network;

import static io.split.android.client.utils.Utils.getAsInt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

class HttpRequestHelper {

    private static final SslProxyConnectionManager sSslProxyManager = new SslProxyConnectionManager();

    /**
     * Executes HTTP requests using custom SSL proxy handling when needed.
     * This method only handles SSL proxy scenarios (PROXY_CACERT, MTLS).
     * For standard HTTP proxies, use the openConnection method instead.
     */
    static HttpResponse executeRequest(@Nullable HttpProxy httpProxy,
                                     @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                                     @NonNull URL url,
                                     @NonNull HttpMethod method,
                                     @NonNull Map<String, String> headers,
                                     @Nullable String body,
                                     boolean useProxyAuthentication,
                                     @Nullable SSLSocketFactory sslSocketFactory) throws IOException {
        
        // Check if we need custom SSL proxy handling
        if (httpProxy != null && sslSocketFactory != null && 
            sSslProxyManager.requiresCustomSslHandling(httpProxy)) {
            
            Logger.v("Using custom SSL proxy handling for auth type: " + httpProxy.getAuthType());
            return sSslProxyManager.executeRequest(httpProxy, url, method, headers, body, sslSocketFactory);
        }
        
        // For non-SSL proxy scenarios, throw UnsupportedOperationException to indicate 
        // that standard HttpURLConnection handling should be used instead
        throw new UnsupportedOperationException("Standard proxy handling should use openConnection method instead");
    }

    static HttpURLConnection openConnection(@Nullable Proxy proxy,
                                            @Nullable HttpProxy httpProxy,
                                            @Nullable SplitUrlConnectionAuthenticator proxyAuthenticator,
                                            @NonNull URL url,
                                            @NonNull HttpMethod method,
                                            @NonNull Map<String, String> headers,
                                            boolean useProxyAuthentication) throws IOException {
        
        // Check if we need custom SSL proxy handling
        if (httpProxy != null && sSslProxyManager.requiresCustomSslHandling(httpProxy)) {
            Logger.v("SSL proxy detected, but openConnection cannot handle it directly");
            throw new IOException("SSL proxy scenarios require custom handling - use executeRequest method instead");
        }
        
        // Standard HttpURLConnection proxy handling
        HttpURLConnection connection;
        if (proxy != null) {
            connection = (HttpURLConnection) url.openConnection(proxy);
            if (useProxyAuthentication && proxyAuthenticator != null) {
                connection = proxyAuthenticator.authenticate(connection);
            }
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod(method.name());
        addHeaders(connection, headers);

        return connection;
    }

    static void applyTimeouts(long readTimeout, long connectionTimeout, HttpURLConnection connection) {
        if (readTimeout > 0) {
            connection.setReadTimeout(getAsInt(readTimeout));
        }

        if (connectionTimeout > 0) {
            connection.setConnectTimeout(getAsInt(connectionTimeout));
        }
    }

    static void applySslConfig(SSLSocketFactory sslSocketFactory, DevelopmentSslConfig developmentSslConfig, HttpURLConnection connection) {
        if (sslSocketFactory != null) {
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            } else {
                Logger.e("Failed to set SSL socket factory.");
            }
        }

        if (developmentSslConfig != null) {
            try {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(developmentSslConfig.getSslSocketFactory());
                    ((HttpsURLConnection) connection).setHostnameVerifier(developmentSslConfig.getHostnameVerifier());
                } else {
                    Logger.e("Failed to set SSL socket factory.");
                }
            } catch (Exception ex) {
                Logger.e("Could not set development SSL config: " + ex.getLocalizedMessage());
            }
        }
    }

    static void checkPins(HttpURLConnection connection, @Nullable CertificateChecker certificateChecker) throws SSLPeerUnverifiedException {
        if (certificateChecker == null || !(connection instanceof HttpsURLConnection)) {
            return;
        }

        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        certificateChecker.checkPins(httpsConnection);
    }

    private static void addHeaders(HttpURLConnection request, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry == null) {
                continue;
            }

            request.addRequestProperty(entry.getKey(), entry.getValue());
        }
    }
}
