package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

/**
 * Establishes HTTP tunnels through proxy using plain socket connections.
 * 
 * This implementation follows the custom tunneling approach:
 * 1. Open plain socket to proxy server
 * 2. Perform manual CONNECT handshake
 * 3. For HTTPS origins: wrap socket in SSLSocket after successful CONNECT
 * 4. For HTTP origins: use plain socket directly
 * 
 * This avoids SSL-over-SSL layering issues by only using SSL where needed.
 */
class PlainSocketTunnelEstablisher {

    /**
     * Establishes a tunnel through the proxy to the target server.
     * 
     * @param proxyHost Proxy server hostname
     * @param proxyPort Proxy server port
     * @param targetHost Target server hostname
     * @param targetPort Target server port
     * @param isHttpsOrigin Whether the target server requires HTTPS
     * @param sslSocketFactory SSL socket factory for HTTPS origins (can be null for HTTP)
     * @return Socket for communicating with target server through tunnel
     * @throws IOException if tunnel establishment fails
     */
    @NonNull
    public Socket establishTunnel(
            @NonNull String proxyHost,
            int proxyPort,
            @NonNull String targetHost,
            int targetPort,
            boolean isHttpsOrigin,
            SSLSocketFactory sslSocketFactory) throws IOException {
        
        Logger.v("Establishing tunnel through proxy using custom tunneling approach");
        Logger.v("Proxy: " + proxyHost + ":" + proxyPort);
        Logger.v("Target: " + targetHost + ":" + targetPort);
        Logger.v("HTTPS origin: " + isHttpsOrigin);
        
        try {
            // Step 1: Open plain socket to proxy server
            Logger.v("Opening plain socket connection to proxy");
            Socket plainSocket = new Socket(proxyHost, proxyPort);
            Logger.v("Plain socket connected to proxy successfully");
            
            // Step 2: Perform manual CONNECT handshake
            Logger.v("Performing manual CONNECT handshake");
            performConnectHandshake(plainSocket, targetHost, targetPort);
            Logger.v("CONNECT handshake completed successfully");
            
            // Step 3: For HTTPS origins, wrap socket in SSLSocket
            if (isHttpsOrigin) {
                if (sslSocketFactory == null) {
                    throw new IOException("SSL socket factory required for HTTPS origins");
                }
                
                Logger.v("Wrapping plain socket in SSLSocket for HTTPS origin");
                SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                    plainSocket,        // Underlying plain socket with tunnel
                    targetHost,         // Target hostname for SNI
                    targetPort,         // Target port
                    false               // Don't auto-close underlying socket
                );
                
                // Configure SSL socket for client mode
                sslSocket.setUseClientMode(true);
                
                Logger.v("SSLSocket created successfully for HTTPS origin");
                Logger.v("SSL socket will perform handshake with target server through tunnel");
                
                return sslSocket;
            } else {
                // Step 4: For HTTP origins, return plain socket directly
                Logger.v("Returning plain socket for HTTP origin");
                return plainSocket;
            }
            
        } catch (Exception e) {
            Logger.e("Custom tunnel establishment failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to establish tunnel using custom tunneling approach", e);
        }
    }
    
    /**
     * Performs manual CONNECT handshake with the proxy server.
     * 
     * @param socket Plain socket connected to proxy
     * @param targetHost Target server hostname
     * @param targetPort Target server port
     * @throws IOException if CONNECT handshake fails
     */
    private void performConnectHandshake(@NonNull Socket socket, 
                                       @NonNull String targetHost, 
                                       int targetPort) throws IOException {
        
        Logger.v("Sending CONNECT request to proxy");
        
        // Create writers and readers for socket communication
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Send CONNECT request
        String connectRequest = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1\r\n" +
                               "Host: " + targetHost + ":" + targetPort + "\r\n" +
                               "Proxy-Connection: keep-alive\r\n" +
                               "\r\n";
        
        Logger.v("CONNECT request: " + connectRequest.replace("\r\n", "\\r\\n"));
        writer.print(connectRequest);
        writer.flush();
        
        // Read and parse CONNECT response
        Logger.v("Reading CONNECT response from proxy");
        String responseLine = reader.readLine();
        
        if (responseLine == null) {
            throw new IOException("No response received from proxy for CONNECT request");
        }
        
        Logger.v("CONNECT response: " + responseLine);
        
        // Parse response status
        if (!responseLine.startsWith("HTTP/1.1 200") && !responseLine.startsWith("HTTP/1.0 200")) {
            throw new IOException("CONNECT request failed: " + responseLine);
        }
        
        // Read remaining headers until empty line
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            Logger.v("CONNECT header: " + headerLine);
        }
        
        Logger.v("CONNECT handshake completed - tunnel established");
    }
}
