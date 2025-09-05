package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
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
                                       @Nullable ProxyCredentialsProvider proxyCredentialsProvider) throws IOException {

        try {
            TunnelConnection connection = establishTunnelConnection(
                    httpProxy, targetUrl, sslSocketFactory, proxyCredentialsProvider, false);
            
            try {
                return mTunnelExecutor.executeRequest(
                        connection.finalSocket,
                        targetUrl,
                        method,
                        headers,
                        body,
                        connection.serverCertificates);
            } finally {
                // Close all sockets for non-streaming requests
                closeConnection(connection);
            }
        } catch (SocketException e) {
            // Let socket-related IOExceptions pass through unwrapped for consistent error handling
            throw e;
        } catch (Exception e) {
            Logger.e("Failed to execute request through custom tunnel: " + e.getMessage());
            throw new IOException("Failed to execute request through custom tunnel", e);
        }
    }

    @NonNull
    HttpStreamResponse executeStreamRequest(@NonNull HttpProxy httpProxy,
                                @NonNull URL targetUrl,
                                @NonNull HttpMethod method,
                                @NonNull Map<String, String> headers,
                                @NonNull SSLSocketFactory sslSocketFactory,
                                @Nullable ProxyCredentialsProvider proxyCredentialsProvider) throws IOException {

        try {
            TunnelConnection connection = establishTunnelConnection(
                    httpProxy, targetUrl, sslSocketFactory, proxyCredentialsProvider, true);
            
            // For streaming requests, pass socket references to the response for later cleanup
            Socket originSocket = (connection.finalSocket != connection.tunnelSocket) ? connection.finalSocket : null;
            return mTunnelExecutor.executeStreamRequest(
                    connection.finalSocket,
                    connection.tunnelSocket,
                    originSocket,
                    targetUrl,
                    method,
                    headers,
                    connection.serverCertificates);
            // For streaming requests, sockets are NOT closed here
            // They will be closed when the HttpStreamResponse.close() is called
        } catch (SocketException e) {
            // Let socket-related IOExceptions pass through unwrapped for consistent error handling
            throw e;
        } catch (Exception e) {
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

    /**
     * Represents a connection through an SSL tunnel.
     */
    private static class TunnelConnection {
        final Socket tunnelSocket;
        final Socket finalSocket;
        final Certificate[] serverCertificates;

        TunnelConnection(Socket tunnelSocket, Socket finalSocket, Certificate[] serverCertificates) {
            this.tunnelSocket = tunnelSocket;
            this.finalSocket = finalSocket;
            this.serverCertificates = serverCertificates;
        }
    }

    /**
     * Establishes a tunnel connection to the target through the proxy.
     *
     * @param httpProxy                The proxy configuration
     * @param targetUrl                The target URL to connect to
     * @param sslSocketFactory         SSL socket factory for connections
     * @param proxyCredentialsProvider Credentials provider for proxy authentication
     * @param isStreaming              Whether this is a streaming connection
     * @return A TunnelConnection object containing the established sockets
     * @throws IOException if connection establishment fails
     */
    private TunnelConnection establishTunnelConnection(
            @NonNull HttpProxy httpProxy,
            @NonNull URL targetUrl,
            @NonNull SSLSocketFactory sslSocketFactory,
            @Nullable ProxyCredentialsProvider proxyCredentialsProvider,
            boolean isStreaming) throws IOException {

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

            finalSocket = tunnelSocket;

            // If the origin is HTTPS, wrap the tunnel socket with a new SSLSocket (system CA)
            if (HTTPS.equalsIgnoreCase(targetUrl.getProtocol())) {
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
                } catch (Exception sslEx) {
                    Logger.e("Failed to establish SSL connection to origin: " + sslEx.getMessage());
                    throw new IOException("Failed to establish SSL connection to origin server", sslEx);
                }
            }

            return new TunnelConnection(tunnelSocket, finalSocket, serverCertificates);
        } catch (Exception e) {
            // Clean up resources on error
            closeSockets(finalSocket, tunnelSocket);
            throw e;
        }
    }

    private void closeConnection(TunnelConnection connection) {
        if (connection == null) {
            return;
        }
        
        closeSockets(connection.finalSocket, connection.tunnelSocket);
    }

    private void closeSockets(Socket finalSocket, Socket tunnelSocket) {
        // If we are tunnelling, finalSocket is the tunnel socket
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
}
