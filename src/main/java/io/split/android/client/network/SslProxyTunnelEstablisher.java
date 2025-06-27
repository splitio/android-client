package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

/**
 * Establishes SSL tunnels to SSL proxies using CONNECT protocol.
 * 
 * CONNECT Specification Compliance:
 * 1. Create raw socket connection to proxy
 * 2. Perform SSL handshake for proxy authentication
 * 3. Send CONNECT request through SSL connection
 * 4. Receive 200 response through SSL connection
 * 5. Return raw socket for transparent tunnel communication
 * 
 * After successful CONNECT, the proxy "blindly forwards data in both directions"
 * and the connection switches to "tunnel mode" with transparent TCP stream relay.
 */
class SslProxyTunnelEstablisher {

    /**
     * Establishes an SSL tunnel through the proxy using the CONNECT method.
     * After successful tunnel establishment, extracts the underlying raw socket
     * for direct use with origin server SSL connections.
     * 
     * @param proxyHost The proxy server hostname
     * @param proxyPort The proxy server port
     * @param targetHost The target server hostname
     * @param targetPort The target server port
     * @param sslSocketFactory SSL socket factory for proxy authentication
     * @return Raw socket with tunnel established (connection maintained)
     * @throws IOException if tunnel establishment fails
     */
    @NonNull
    public Socket establishTunnel(@NonNull String proxyHost,
                                      int proxyPort,
                                      @NonNull String targetHost,
                                      int targetPort,
                                      @NonNull SSLSocketFactory sslSocketFactory) throws IOException {
        
        Logger.v("Establishing SSL tunnel through proxy: " + proxyHost + ":" + proxyPort + 
                 " to target: " + targetHost + ":" + targetPort);
        
        Socket rawSocket = null;
        SSLSocket sslSocket = null;
        
        try {
            // Step 1: Create raw TCP connection to proxy
            Logger.v("Creating raw TCP connection to proxy");
            rawSocket = new Socket(proxyHost, proxyPort);
            rawSocket.setSoTimeout(10000); // 10 second timeout
            
            // Step 2: Establish SSL connection to proxy for mTLS authentication
            Logger.v("Establishing SSL connection to proxy for mTLS authentication");
            
            // Create a temporary SSL socket to establish the SSL session with proper trust validation
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(rawSocket, proxyHost, proxyPort, false);
            sslSocket.setUseClientMode(true);
            sslSocket.setSoTimeout(10000); // 10 second timeout
            
            // Perform SSL handshake using the SSL socket with custom CA certificates
            Logger.v("Performing SSL handshake with proxy");
            sslSocket.startHandshake();
            
            // Step 3: Send CONNECT request through SSL connection
            Logger.v("Sending CONNECT request through SSL connection");
            sendConnectRequest(sslSocket, targetHost, targetPort);
            
            // Step 4: Validate CONNECT response through SSL connection
            validateConnectResponse(sslSocket, targetHost, targetPort);
            Logger.v("SSL tunnel established successfully");
            
            // Step 5: Return SSL socket for tunnel communication
            // The SSL socket has the CONNECT tunnel established and can be used directly
            Logger.v("SSL tunnel established successfully - returning SSL socket");
            return sslSocket;
            
        } catch (Exception e) {
            Logger.e("SSL tunnel establishment failed: " + e.getMessage());
            e.printStackTrace();
            
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
            
            if (e instanceof IOException) {
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
                                   int targetPort) throws Exception {
        
        // Build CONNECT request
        String connectRequest = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1\r\n" +
                               "Host: " + targetHost + ":" + targetPort + "\r\n" +
                               "\r\n";
        
        Logger.v("Sending CONNECT request through SSL: CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1");
        
        // Encrypt and send CONNECT request
        byte[] requestBytes = connectRequest.getBytes("UTF-8");
        sslSocket.getOutputStream().write(requestBytes);
        sslSocket.getOutputStream().flush();
        
        Logger.v("CONNECT request sent through SSL connection");
    }
    
    /**
     * Validates CONNECT response through SSL connection.
     */
    private void validateConnectResponse(@NonNull SSLSocket sslSocket,
                                       @NonNull String targetHost,
                                       int targetPort) throws Exception {
        
        // Read encrypted response and decrypt it
        byte[] responseBytes = new byte[1024];
        int bytesRead = sslSocket.getInputStream().read(responseBytes);
        if (bytesRead > 0) {
            String response = new String(responseBytes, 0, bytesRead, "UTF-8");
            Logger.v("Received CONNECT response through SSL: " + response.trim());
            
            // Parse status line
            String[] lines = response.split("\r\n");
            if (lines.length == 0) {
                throw new IOException("No CONNECT response received from proxy");
            }
            
            String statusLine = lines[0];
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
            
            if (statusCode != 200) {
                throw new IOException("CONNECT request failed with status " + statusCode + ": " + statusLine);
            }
            
            Logger.v("CONNECT request successful - proxy switched to tunnel mode");
        } else {
            throw new IOException("No CONNECT response received from proxy");
        }
    }
}
