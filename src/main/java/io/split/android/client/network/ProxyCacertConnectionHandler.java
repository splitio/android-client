package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

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

    private final HttpOverTunnelExecutor mTunnelExecutor;

    public ProxyCacertConnectionHandler() {
        mTunnelExecutor = new HttpOverTunnelExecutor();
    }

    // For testing - allow injection of dependencies
    ProxyCacertConnectionHandler(HttpOverTunnelExecutor tunnelExecutor) {
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
        
        Logger.v("PROXY_CACERT: Executing request to: " + targetUrl);
        
        try {
            // PROXY_CACERT requires SSL authentication with proxy using CA certificate
            // Use the provided sslSocketFactory which contains the proxy CA certificate
            
            // Establish SSL tunnel through proxy with CA certificate authentication
            SslProxyTunnelEstablisher tunnelEstablisher = new SslProxyTunnelEstablisher();
            Socket tunnelSocket = tunnelEstablisher.establishTunnel(
                httpProxy.getHost(),
                httpProxy.getPort(),
                targetUrl.getHost(),
                getTargetPort(targetUrl),
                sslSocketFactory  // Use the SSL socket factory with proxy CA certificate
            );
            
            Logger.v("SSL tunnel established successfully");
            
            Socket finalSocket = tunnelSocket;
            // If the origin is HTTPS, wrap the tunnel socket with a new SSLSocket (system CA)
            if ("https".equalsIgnoreCase(targetUrl.getProtocol())) {
                Logger.v("Wrapping tunnel socket with new SSLSocket for origin server handshake");
                try {
                    // Get system default SSL context
                    javax.net.ssl.SSLContext systemSslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                    systemSslContext.init(null, null, null); // null = system default trust managers
                    javax.net.ssl.SSLSocketFactory systemSslSocketFactory = systemSslContext.getSocketFactory();

                    // Create SSLSocket layered over tunnel
                    finalSocket = systemSslSocketFactory.createSocket(
                        tunnelSocket,
                        targetUrl.getHost(),
                        getTargetPort(targetUrl),
                        true // autoClose
                    );
                    if (finalSocket instanceof javax.net.ssl.SSLSocket) {
                        javax.net.ssl.SSLSocket originSslSocket = (javax.net.ssl.SSLSocket) finalSocket;
                        originSslSocket.setUseClientMode(true);
                        originSslSocket.startHandshake();
                        try {
                            java.security.cert.Certificate[] peerCerts = originSslSocket.getSession().getPeerCertificates();
                            for (java.security.cert.Certificate cert : peerCerts) {
                                if (cert instanceof java.security.cert.X509Certificate) {
                                    java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cert;
                                    Logger.v("Origin SSL handshake: Peer cert subject=" + x509.getSubjectX500Principal() + ", issuer=" + x509.getIssuerX500Principal());
                                }
                            }
                        } catch (Exception certEx) {
                            Logger.e("Could not log origin server certificates: " + certEx.getMessage());
                        }
                    } else {
                        throw new IOException("Failed to create SSLSocket to origin");
                    }
                    Logger.v("SSL handshake with origin server completed");
                } catch (Exception sslEx) {
                    Logger.e("Failed to establish SSL connection to origin: " + sslEx.getMessage());
                    throw new IOException("Failed to establish SSL connection to origin server", sslEx);
                }
            }

            // Execute request through the (possibly wrapped) socket
            HttpResponse response = mTunnelExecutor.executeRequest(
                finalSocket,
                targetUrl,
                method,
                headers,
                body
            );

            Logger.v("PROXY_CACERT request completed successfully, status: " + response.getHttpStatus());
            return response;
            
        } catch (Exception e) {
            Logger.e("PROXY_CACERT request failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to execute request through custom tunnel", e);
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
