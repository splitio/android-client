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
     * Establishes an SSL tunnel to the specified target through an SSL proxy.
     * 
     * @param proxyHost The SSL proxy hostname
     * @param proxyPort The SSL proxy port
     * @param targetHost The target server hostname
     * @param targetPort The target server port
     * @param sslSocketFactory SSL socket factory for proxy authentication
     * @return SSL socket with tunnel established (connection maintained)
     * @throws IOException if tunnel establishment fails
     */
    @NonNull
    public SSLSocket establishTunnel(@NonNull String proxyHost,
                                    int proxyPort,
                                    @NonNull String targetHost,
                                    int targetPort,
                                    @NonNull SSLSocketFactory sslSocketFactory) throws IOException {
        
        System.out.println("=== SSL TUNNEL ESTABLISHMENT DEBUG ===");
        System.out.println("Establishing SSL tunnel to " + targetHost + ":" + targetPort + 
                 " through SSL proxy " + proxyHost + ":" + proxyPort);
        
        Socket rawSocket = null;
        SSLSocket sslSocket = null;
        
        try {
            // Step 1: Create raw socket connection to proxy
            System.out.println("Step 1: Creating raw socket connection to proxy...");
            rawSocket = new Socket(proxyHost, proxyPort);
            rawSocket.setSoTimeout(10000); // 10 second timeout
            System.out.println("Raw socket connected to proxy: " + proxyHost + ":" + proxyPort);
            
            // Step 2: Create SSL engine for proxy authentication using custom CA certificates
            System.out.println("Step 2: Creating SSL socket for proxy authentication...");
            
            // We need to use the provided SSLSocketFactory which contains the custom CA certificates
            // Create a temporary SSL socket to establish the SSL session with proper trust validation
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(rawSocket, proxyHost, proxyPort, false);
            sslSocket.setUseClientMode(true);
            sslSocket.setSoTimeout(10000); // 10 second timeout
            System.out.println("SSL socket created successfully");
            
            // Perform SSL handshake using the SSL socket with custom CA certificates
            System.out.println("Performing SSL handshake with custom CA certificates...");
            System.out.println("About to call sslSocket.startHandshake()...");
            sslSocket.startHandshake();
            System.out.println("SSL handshake completed successfully with custom CA validation");
            
            // Now we have an established SSL connection with proper certificate validation
            // We'll use this SSL socket directly for CONNECT protocol instead of SSLEngine
            System.out.println("SSL socket created successfully with custom CA validation");
            
            // Step 3: Send CONNECT request through SSL connection
            System.out.println("Step 3: Sending CONNECT request through SSL connection...");
            sendConnectRequest(sslSocket, targetHost, targetPort);
            System.out.println("CONNECT request sent successfully");
            
            // Step 4: Validate CONNECT response through SSL connection
            System.out.println("Step 4: Validating CONNECT response...");
            validateConnectResponse(sslSocket, targetHost, targetPort);
            System.out.println("CONNECT response validated successfully");
            
            System.out.println("SSL tunnel established successfully - proxy switched to tunnel mode");
            System.out.println("Returning SSL socket for transparent tunnel communication");
            System.out.println("=== SSL TUNNEL ESTABLISHMENT COMPLETE ===");
            
            // Step 5: Return SSL socket for transparent tunnel communication
            // After CONNECT success, the SSL socket connection is established and ready
            // The proxy will now blindly forward data through this connection
            // IMPORTANT: We return the SSL socket, not the raw socket, because it maintains the connection
            System.out.println("Returning SSL socket (not raw socket) to maintain connection");
            return sslSocket;
            
        } catch (Exception e) {
            System.out.println("=== SSL TUNNEL ESTABLISHMENT FAILED ===");
            System.out.println("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Clean up on failure
            if (rawSocket != null) {
                try {
                    rawSocket.close();
                } catch (IOException closeEx) {
                    System.out.println("Failed to close raw socket during cleanup: " + closeEx.getMessage());
                }
            }
            if (sslSocket != null) {
                try {
                    sslSocket.close();
                } catch (IOException closeEx) {
                    System.out.println("Failed to close SSL socket during cleanup: " + closeEx.getMessage());
                }
            }
            throw new IOException("Failed to establish SSL tunnel to " + targetHost + ":" + targetPort, e);
        }
        // NOTE: We return the SSL socket which maintains the connection
        // The raw socket cleanup is handled by the SSL socket
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
