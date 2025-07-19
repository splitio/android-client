package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Map;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

/**
 * Handles PROXY_CACERT SSL proxy connections.
 * <p>
 * This handler establishes SSL tunnels through SSL proxies using custom CA certificates
 * for proxy authentication, then executes HTTP requests through the SSL tunnel.
 */
class ProxyCacertConnectionHandler {

    public static final String HTTPS = "https";
    public static final String HTTP = "http";
    public static final int PORT_HTTPS = 443;
    public static final int PORT_HTTP = 80;
    private final HttpOverTunnelExecutor mTunnelExecutor;

    public ProxyCacertConnectionHandler() {
        mTunnelExecutor = new HttpOverTunnelExecutor();
    }

    /**
     * Executes an HTTP request through an SSL proxy tunnel.
     *
     * @param httpProxy                The proxy configuration
     * @param targetUrl                The target URL to connect to
     * @param method                   The HTTP method to use
     * @param headers                  The HTTP headers to include
     * @param body                     The request body (if any)
     * @param sslSocketFactory         The SSL socket factory for proxy and origin connections
     * @param proxyCredentialsProvider Credentials provider for proxy authentication
     * @param isStreaming              Whether this connection is for streaming (uses longer timeout)
     * @return The HTTP response
     * @throws IOException if the request fails
     */
    @NonNull
    HttpResponse executeRequest(@NonNull HttpProxy httpProxy,
                                       @NonNull URL targetUrl,
                                       @NonNull HttpMethod method,
                                       @NonNull Map<String, String> headers,
                                       @Nullable String body,
                                       @NonNull SSLSocketFactory sslSocketFactory,
                                       @Nullable ProxyCredentialsProvider proxyCredentialsProvider,
                                       boolean isStreaming) throws IOException {

        try {
            SslProxyTunnelEstablisher tunnelEstablisher = new SslProxyTunnelEstablisher();
            Socket tunnelSocket = null;
            Socket finalSocket = null;
            Certificate[] serverCertificates = null;

            try {
                tunnelSocket = tunnelEstablisher.establishTunnel(
                        httpProxy.getHost(),
                        httpProxy.getPort(),
                        targetUrl.getHost(),
                        getTargetPort(targetUrl),
                        sslSocketFactory,
                        proxyCredentialsProvider,
                        isStreaming
                );

                Logger.v("SSL tunnel established successfully");

                finalSocket = tunnelSocket;

                // If the origin is HTTPS, wrap the tunnel socket with a new SSLSocket (system CA)
                if (HTTPS.equalsIgnoreCase(targetUrl.getProtocol())) {
                    Logger.v("Wrapping tunnel socket with new SSLSocket for origin server handshake");
                    try {
                        // Use the provided SSLSocketFactory, which is configured to trust the origin's CA
                        finalSocket = sslSocketFactory.createSocket(
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

                return mTunnelExecutor.executeRequest(
                        finalSocket,
                        targetUrl,
                        method,
                        headers,
                        body,
                        serverCertificates
                );
            } finally {
                // If we have are tunelling, finalSocket is the tunnel socket
                if (finalSocket != null && finalSocket != tunnelSocket) {
                    try {
                        finalSocket.close();
                    } catch (IOException e) {
                        Logger.w("Failed to close origin SSL socket: " + e.getMessage());
                    }
                }

                if (tunnelSocket != null) {
                    try {
                        tunnelSocket.close();
                    } catch (IOException e) {
                        Logger.w("Failed to close tunnel socket: " + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            // Let socket-related IOExceptions pass through unwrapped for consistent error handling
            throw e;
        } catch (IOException e) {
            throw new IOException("Failed to execute request through custom tunnel", e);
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
                return PORT_HTTP;
            }
        }
        return port;
    }

}
