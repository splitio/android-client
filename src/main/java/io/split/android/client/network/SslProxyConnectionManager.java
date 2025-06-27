package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

/**
 * Factory/manager for creating appropriate SSL proxy connection handlers.
 * Routes SSL proxy requests to the correct handler based on proxy authentication type.
 */
class SslProxyConnectionManager {
    
    private final List<SslProxyConnectionHandler> mHandlers;
    
    public SslProxyConnectionManager() {
        mHandlers = Arrays.asList(
            new ProxyCacertConnectionHandler()
            // MtlsProxyConnectionHandler will be added later
        );
    }
    
    // For testing - allow injection of custom handlers
    SslProxyConnectionManager(List<SslProxyConnectionHandler> handlers) {
        mHandlers = handlers;
    }
    
    /**
     * Determines if the given proxy requires custom SSL proxy handling.
     * 
     * @param proxy The HttpProxy configuration to check
     * @return true if custom SSL proxy handling is needed
     */
    public boolean requiresCustomSslHandling(@Nullable HttpProxy proxy) {
        if (proxy == null) {
            return false;
        }
        
        for (SslProxyConnectionHandler handler : mHandlers) {
            if (handler.canHandle(proxy)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Executes an HTTP request using the appropriate SSL proxy handler.
     * 
     * @param proxy The SSL proxy configuration
     * @param targetUrl The final destination URL
     * @param method HTTP method for the request
     * @param headers Headers to include in the request
     * @param body Request body (null for GET requests)
     * @param sslSocketFactory SSL socket factory configured for proxy authentication
     * @return HttpResponse containing the server's response
     * @throws IOException if no handler can handle the proxy or if request execution fails
     */
    @NonNull
    public HttpResponse executeRequest(
            @NonNull HttpProxy proxy,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body,
            @NonNull SSLSocketFactory sslSocketFactory) throws IOException {
        
        Logger.v("Looking for SSL proxy handler for auth type: " + proxy.getAuthType());
        
        // Find appropriate handler
        for (SslProxyConnectionHandler handler : mHandlers) {
            if (handler.canHandle(proxy)) {
                Logger.v("Using handler: " + handler.getClass().getSimpleName());
                return handler.executeRequest(proxy, targetUrl, method, headers, body, sslSocketFactory);
            }
        }
        
        throw new IOException("No SSL proxy handler available for proxy auth type: " + proxy.getAuthType());
    }
}
