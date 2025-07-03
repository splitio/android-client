package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.Socket;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
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

    public static final String HTTPS = "https";
    public static final String HTTP = "http";
    public static final int PORT_HTTPS = 443;
    public static final int HTTP_PORT = 80;
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
        return httpProxy.getAuthType() == HttpProxy.ProxyAuthType.PROXY_CACERT || httpProxy.getAuthType() == HttpProxy.ProxyAuthType.MTLS;
    }

    @Override
    @NonNull
    public HttpResponse executeRequest(@NonNull HttpProxy httpProxy,
                                       @NonNull URL targetUrl,
                                       @NonNull HttpMethod method,
                                       @NonNull Map<String, String> headers,
                                       @Nullable String body,
                                       @NonNull SSLSocketFactory sslSocketFactory,
                                       @Nullable ProxyCredentialsProvider proxyCredentialsProvider) throws IOException {
        
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
                sslSocketFactory,  // Use the SSL socket factory with proxy CA certificate,
                proxyCredentialsProvider
            );
            
            Logger.v("SSL tunnel established successfully");
            
            Socket finalSocket = tunnelSocket;
            Certificate[] serverCertificates = null;
            
            // If the origin is HTTPS, wrap the tunnel socket with a new SSLSocket (system CA)
            if ("https".equalsIgnoreCase(targetUrl.getProtocol())) {
                Logger.v("Wrapping tunnel socket with new SSLSocket for origin server handshake");
                try {
                    // Get system default SSL context
                    SSLContext systemSslContext = SSLContext.getInstance("TLS");
                    systemSslContext.init(null, null, null); // null = system default trust managers
                    SSLSocketFactory systemSslSocketFactory = systemSslContext.getSocketFactory();

                    // Create SSLSocket layered over tunnel
                    finalSocket = systemSslSocketFactory.createSocket(
                        tunnelSocket,
                        targetUrl.getHost(),
                        getTargetPort(targetUrl),
                        true // autoClose
                    );
                    if (finalSocket instanceof SSLSocket) {
                        SSLSocket originSslSocket = (SSLSocket) finalSocket;
                        originSslSocket.setUseClientMode(true);
                        originSslSocket.startHandshake();
                        
                        // Capture server certificates after successful handshake
                        try {
                            serverCertificates = originSslSocket.getSession().getPeerCertificates();
                            Logger.v("Captured " + (serverCertificates != null ? serverCertificates.length : 0) + " certificates from origin server");
                        } catch (Exception certEx) {
                            Logger.w("Could not capture origin server certificates: " + certEx.getMessage());
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

            // Execute request through the (possibly wrapped) socket, passing the certificates
            HttpResponse response = mTunnelExecutor.executeRequest(
                finalSocket,
                targetUrl,
                method,
                headers,
                body,
                serverCertificates
            );

            Logger.v("PROXY_CACERT request completed successfully, status: " + response.getHttpStatus());
            return response;
            
        } catch (Exception e) {
            if (e instanceof HttpRetryException) {
                throw (HttpRetryException) e;
            }
            throw new IOException("Failed to execute request through custom tunnel", e);
        }
    }

    private static int getTargetPort(@NonNull URL targetUrl) {
        int port = targetUrl.getPort();
        if (port == -1) {
            if (HTTPS.equalsIgnoreCase(targetUrl.getProtocol())) {
                return PORT_HTTPS;
            } else if (HTTP.equalsIgnoreCase(targetUrl.getProtocol())) {
                return HTTP_PORT;
            }
        }
        return port;
    }
}
