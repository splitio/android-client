package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

/**
 * Handles PROXY_CACERT SSL proxy connections.
 * 
 * This handler establishes SSL tunnels through SSL proxies using custom CA certificates
 * for proxy authentication, then executes HTTP requests through the SSL tunnel.
 * 
 * CONNECT Specification Compliance:
 * - Establishes SSL connection to proxy for authentication
 * - Sends CONNECT request through SSL connection
 * - Maintains SSL socket connection after successful CONNECT
 * - Executes HTTP requests through SSL tunnel socket
 */
class ProxyCacertConnectionHandler implements SslProxyConnectionHandler {

    private final SslProxyTunnelEstablisher mTunnelEstablisher;
    private final HttpOverTunnelExecutor mTunnelExecutor;

    public ProxyCacertConnectionHandler() {
        mTunnelEstablisher = new SslProxyTunnelEstablisher();
        mTunnelExecutor = new HttpOverTunnelExecutor();
    }

    // For testing - allow injection of dependencies
    ProxyCacertConnectionHandler(SslProxyTunnelEstablisher tunnelEstablisher, 
                                HttpOverTunnelExecutor tunnelExecutor) {
        mTunnelEstablisher = tunnelEstablisher;
        mTunnelExecutor = tunnelExecutor;
    }

    @Override
    public boolean canHandle(@NonNull HttpProxy httpProxy) {
        return httpProxy.getAuthType() == HttpProxy.ProxyAuthType.PROXY_CACERT;
    }

    @Override
    @NonNull
    public HttpResponse executeRequest(@NonNull HttpProxy httpProxy,
                                     @NonNull URL targetUrl,
                                     @NonNull HttpMethod method,
                                     @NonNull Map<String, String> headers,
                                     @Nullable String body,
                                     @NonNull SSLSocketFactory sslSocketFactory) throws IOException {
        
        Logger.v("Executing request through PROXY_CACERT SSL proxy: " + 
                 httpProxy.getHost() + ":" + httpProxy.getPort());
        
        SSLSocket tunnelSocket = null;
        try {
            // Step 1: Establish SSL tunnel through proxy with CA certificate validation
            tunnelSocket = mTunnelEstablisher.establishTunnel(
                httpProxy.getHost(),
                httpProxy.getPort(),
                targetUrl.getHost(),
                getTargetPort(targetUrl),
                sslSocketFactory
            );
            
            Logger.v("SSL tunnel established successfully - connection maintained");
            
            // Step 2: Execute HTTP request through SSL tunnel socket
            HttpResponse response = mTunnelExecutor.executeRequest(
                tunnelSocket,
                targetUrl,
                method,
                headers,
                body,
                sslSocketFactory  // Pass the combined SSL socket factory for HTTPS origins
            );
            
            Logger.v("PROXY_CACERT request completed successfully, status: " + response.getHttpStatus());
            return response;
            
        } finally {
            // Clean up tunnel socket
            if (tunnelSocket != null) {
                try {
                    tunnelSocket.close();
                } catch (IOException e) {
                    Logger.w("Failed to close tunnel socket: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets the target port from URL, defaulting based on protocol.
     */
    private int getTargetPort(@NonNull URL targetUrl) {
        int port = targetUrl.getPort();
        if (port == -1) {
            if ("https".equalsIgnoreCase(targetUrl.getProtocol())) {
                return 443;
            } else if ("http".equalsIgnoreCase(targetUrl.getProtocol())) {
                return 80;
            }
        }
        return port;
    }
}
