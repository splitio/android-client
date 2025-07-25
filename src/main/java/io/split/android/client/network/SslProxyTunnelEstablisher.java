package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Establishes SSL tunnels to SSL proxies using CONNECT protocol.
 */
class SslProxyTunnelEstablisher {

    private static final String CRLF = "\r\n";
    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
    private final Base64Encoder mBase64Encoder;

    // Default timeout for regular connections (10 seconds)
    private static final int DEFAULT_SOCKET_TIMEOUT = 20000;

    SslProxyTunnelEstablisher() {
        this(new DefaultBase64Encoder());
    }

    @VisibleForTesting
    SslProxyTunnelEstablisher(Base64Encoder base64Encoder) {
        mBase64Encoder = base64Encoder;
    }

    /**
     * Establishes an SSL tunnel through the proxy using the CONNECT method.
     * After successful tunnel establishment, extracts the underlying socket
     * for use with origin server SSL connections.
     *
     * @param proxyHost                The proxy server hostname
     * @param proxyPort                The proxy server port
     * @param targetHost               The target server hostname
     * @param targetPort               The target server port
     * @param sslSocketFactory         SSL socket factory for proxy authentication
     * @param proxyCredentialsProvider Credentials provider for proxy authentication
     * @param isStreaming              Whether this connection is for streaming (uses longer timeout)
     * @return Raw socket with tunnel established (connection maintained)
     * @throws IOException if tunnel establishment fails
     */
    @NonNull
    Socket establishTunnel(@NonNull String proxyHost,
                           int proxyPort,
                           @NonNull String targetHost,
                           int targetPort,
                           @NonNull SSLSocketFactory sslSocketFactory,
                           @Nullable ProxyCredentialsProvider proxyCredentialsProvider,
                           boolean isStreaming) throws IOException {

        Socket rawSocket = null;
        SSLSocket sslSocket = null;

        try {
            int timeout = DEFAULT_SOCKET_TIMEOUT;
            // Step 1: Create raw TCP connection to proxy
            rawSocket = new Socket(proxyHost, proxyPort);
            rawSocket.setSoTimeout(timeout);

            // Create a temporary SSL socket to establish the SSL session with proper trust validation
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(rawSocket, proxyHost, proxyPort, true);
            sslSocket.setUseClientMode(true);
            if (isStreaming) {
                sslSocket.setSoTimeout(0); // no timeout for streaming
            } else {
                sslSocket.setSoTimeout(timeout);
            }

            // Perform SSL handshake using the SSL socket with custom CA certificates
            sslSocket.startHandshake();

            // Validate the proxy hostname
//            HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
//            if (!verifier.verify(proxyHost, sslSocket.getSession())) {
//                throw new SSLHandshakeException("Proxy hostname verification failed");
//            }

            // Step 3: Send CONNECT request through SSL connection
            sendConnectRequest(sslSocket, targetHost, targetPort, proxyCredentialsProvider);

            // Step 4: Validate CONNECT response through SSL connection
            validateConnectResponse(sslSocket);

            // Step 5: Return SSL socket for tunnel communication
            return sslSocket;

        } catch (Exception e) {
            // Clean up resources on error
            if (sslSocket != null) {
                try {
                    sslSocket.close();
                } catch (IOException closeEx) {
                    // Ignore close exceptions
                }
            } else if (rawSocket != null) {
                try {
                    rawSocket.close();
                } catch (IOException closeEx) {
                    // Ignore close exceptions
                }
            }

            if (e instanceof HttpRetryException) {
                throw (HttpRetryException) e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException("Failed to establish SSL tunnel", e);
            }
        }
    }

    /**
     * Sends CONNECT request through SSL connection to proxy.
     */
    private void sendConnectRequest(@NonNull SSLSocket sslSocket,
                                    @NonNull String targetHost,
                                    int targetPort,
                                    @Nullable ProxyCredentialsProvider proxyCredentialsProvider) throws IOException {

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), false);
        writer.write("CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1" + CRLF);
        writer.write("Host: " + targetHost + ":" + targetPort + CRLF);

        if (proxyCredentialsProvider != null) {
            addProxyAuthHeader(proxyCredentialsProvider, writer);
        }

        // Send empty line to end headers
        writer.write(CRLF);
        writer.flush();
    }

    private void addProxyAuthHeader(@NonNull ProxyCredentialsProvider proxyCredentialsProvider, PrintWriter writer) {
        if (proxyCredentialsProvider instanceof BearerCredentialsProvider) {
            // Send Proxy-Authorization header if credentials are set
            String bearerToken = ((BearerCredentialsProvider) proxyCredentialsProvider).getToken();
            if (bearerToken != null && !bearerToken.trim().isEmpty()) {
                writer.write(PROXY_AUTHORIZATION_HEADER + ": Bearer " + bearerToken + CRLF);
            }
        } else if (proxyCredentialsProvider instanceof BasicCredentialsProvider) {
            BasicCredentialsProvider basicCredentialsProvider = (BasicCredentialsProvider) proxyCredentialsProvider;
            String userName = basicCredentialsProvider.getUsername();
            String password = basicCredentialsProvider.getPassword();
            if (userName != null && !userName.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
                writer.write(PROXY_AUTHORIZATION_HEADER + ": Basic " + mBase64Encoder.encode(userName + ":" + password) + CRLF);
            }
        }
    }

    /**
     * Validates CONNECT response through SSL connection.
     * Only reads status line and headers, leaving the stream open for tunneling.
     */
    private void validateConnectResponse(@NonNull SSLSocket sslSocket) throws IOException {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = reader.readLine();
            if (statusLine == null) {
                throw new IOException("No CONNECT response received from proxy");
            }

            // Parse status code
            String[] statusParts = statusLine.split(" ");
            if (statusParts.length < 2) {
                throw new IOException("Invalid CONNECT response status line: " + statusLine);
            }

            int statusCode;
            try {
                statusCode = Integer.parseInt(statusParts[1]);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid CONNECT response status code: " + statusLine, e);
            }

            // Read headers until empty line (but don't process them for CONNECT)
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.trim().isEmpty()) {
                // no-op
            }

            // Check status code
            if (statusCode != 200) {
                if (statusCode == HttpURLConnection.HTTP_PROXY_AUTH) {
                    throw new HttpRetryException("CONNECT request failed with status " + statusCode + ": " + statusLine, HttpURLConnection.HTTP_PROXY_AUTH);
                }
                throw new IOException("CONNECT request failed with status " + statusCode + ": " + statusLine);
            }
        } catch (IOException e) {
            if (e instanceof HttpRetryException) {
                throw e;
            }

            throw new IOException("Failed to validate CONNECT response from proxy: " + e.getMessage(), e);
        }
    }
}
