package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.split.android.client.utils.logger.Logger;

/**
 * Executes HTTP requests through an established SSL tunnel socket.
 * 
 * After CONNECT tunnel establishment, the SSL socket maintains the connection
 * and handles transparent tunnel communication with the proxy.
 * 
 * This component sends HTTP requests through the SSL socket streams,
 * which are transparently forwarded by the proxy to the origin server.
 */
class HttpOverTunnelExecutor {

    private final HttpResponseParser mResponseParser;

    public HttpOverTunnelExecutor() {
        mResponseParser = new HttpResponseParser();
    }

    // For testing - allow injection of custom parser
    HttpOverTunnelExecutor(HttpResponseParser responseParser) {
        mResponseParser = responseParser;
    }

    /**
     * Executes an HTTP request through the established SSL tunnel socket.
     * 
     * @param tunnelSocket The SSL socket with tunnel established (connection maintained)
     * @param targetUrl The final destination URL (HTTP or HTTPS)
     * @param method HTTP method for the request
     * @param headers Headers to include in the request
     * @param body Request body (null for GET requests)
     * @param combinedSslSocketFactory The combined SSL socket factory to use for HTTPS origins
     * @return HttpResponse containing the server's response
     * @throws IOException if the request execution fails
     */
    @NonNull
    public HttpResponse executeRequest(
            @NonNull SSLSocket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body,
            @NonNull SSLSocketFactory combinedSslSocketFactory) throws IOException {
        
        Logger.v("Executing request through SSL tunnel to: " + targetUrl);
        
        try {
            if ("https".equalsIgnoreCase(targetUrl.getProtocol())) {
                // For HTTPS origins, we still need to establish SSL connection to origin
                // through the tunnel using the combined SSL socket factory
                return executeHttpsRequest(tunnelSocket, targetUrl, method, headers, body, combinedSslSocketFactory);
            } else {
                // For HTTP origins, send request directly through tunnel
                return executeHttpRequest(tunnelSocket, targetUrl, method, headers, body);
            }
        } catch (Exception e) {
            Logger.e("Failed to execute request through tunnel: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to execute HTTP request through tunnel to " + targetUrl, e);
        }
    }
    
    /**
     * Executes HTTPS request by establishing SSL connection to origin through tunnel.
     */
    @NonNull
    private HttpResponse executeHttpsRequest(
            @NonNull SSLSocket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body,
            @NonNull SSLSocketFactory combinedSslSocketFactory) throws IOException {
        
        Logger.v("Establishing SSL connection to HTTPS origin through tunnel");
        
        // After CONNECT, the tunnel is transparent and we need to establish
        // a new SSL connection to the origin server through the tunnel.
        // This implements the "onion layering" architecture:
        // - Outer layer: SSL connection to proxy (already established)
        // - Inner layer: SSL connection to origin (through tunnel)
        
        try {
            // Use the same combined SSL socket factory that was used for the tunnel
            // This factory contains both proxy CA and origin CA certificates
            // so it can validate both proxy and origin server certificates
            
            // Create SSL socket to origin server using tunnel socket as the underlying transport
            // This uses the correct SSLSocketFactory.createSocket(Socket, String, int, boolean) method
            javax.net.ssl.SSLSocket originSslSocket = (javax.net.ssl.SSLSocket) 
                combinedSslSocketFactory.createSocket(
                    tunnelSocket,           // Use tunnel socket as underlying transport
                    targetUrl.getHost(),    // Origin server hostname
                    getTargetPort(targetUrl), // Origin server port
                    false                   // Don't auto-close underlying socket
                );
            
            // Configure SSL socket for client mode
            originSslSocket.setUseClientMode(true);
            
            // Perform SSL handshake with origin server through tunnel
            Logger.v("Performing SSL handshake with origin server through tunnel");
            originSslSocket.startHandshake();
            Logger.v("SSL handshake with origin server completed successfully");
            
            // Now send HTTP request through the SSL connection to origin
            sendHttpRequest(originSslSocket, targetUrl, method, headers, body);
            
            // Read and parse HTTP response from origin
            HttpResponse response = mResponseParser.parseHttpResponse(originSslSocket.getInputStream());
            
            Logger.v("HTTPS request completed successfully, status: " + response.getHttpStatus());
            return response;
            
        } catch (Exception e) {
            Logger.e("Failed to establish SSL connection to origin through tunnel: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("SSL handshake with origin server failed", e);
        }
    }
    
    /**
     * Executes HTTP request directly through transparent tunnel (for HTTP origins).
     */
    @NonNull
    private HttpResponse executeHttpRequest(
            @NonNull SSLSocket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body) throws IOException {
        
        Logger.v("Sending HTTP request directly through transparent tunnel");
        
        // Send HTTP request through transparent tunnel
        sendHttpRequest(tunnelSocket, targetUrl, method, headers, body);
        
        // Read and parse HTTP response
        return mResponseParser.parseHttpResponse(tunnelSocket.getInputStream());
    }
    
    /**
     * Sends the HTTP request through the tunnel socket.
     */
    private void sendHttpRequest(
            @NonNull SSLSocket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body) throws IOException {
        
        PrintWriter writer = new PrintWriter(tunnelSocket.getOutputStream(), true);
        
        // 1. Send request line
        String path = targetUrl.getPath();
        if (path.isEmpty()) {
            path = "/";
        }
        if (targetUrl.getQuery() != null) {
            path += "?" + targetUrl.getQuery();
        }
        
        String requestLine = method.name() + " " + path + " HTTP/1.1";
        Logger.v("Sending request line: '" + requestLine + "'");
        writer.println(requestLine);
        
        // 2. Send Host header (required for HTTP/1.1)
        String host = targetUrl.getHost();
        int port = getTargetPort(targetUrl);
        
        // Add port to Host header if it's not the default port for the protocol
        boolean isDefaultPort = ("http".equalsIgnoreCase(targetUrl.getProtocol()) && port == 80) ||
                               ("https".equalsIgnoreCase(targetUrl.getProtocol()) && port == 443);
        
        if (!isDefaultPort) {
            host += ":" + port;
        }
        
        Logger.v("Sending Host header: 'Host: " + host + "'");
        writer.println("Host: " + host);
        
        // 3. Send custom headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() != null && header.getValue() != null) {
                String headerLine = header.getKey() + ": " + header.getValue();
                Logger.v("Sending header: '" + headerLine + "'");
                writer.println(headerLine);
            }
        }
        
        // 4. Send Content-Length header if body is present
        if (body != null) {
            String contentLengthHeader = "Content-Length: " + body.length();
            Logger.v("Sending header: '" + contentLengthHeader + "'");
            writer.println(contentLengthHeader);
        }
        
        // 5. Send Connection: close to ensure response completion
        Logger.v("Sending header: 'Connection: close'");
        writer.println("Connection: close");
        
        // 6. End headers with empty line
        Logger.v("Sending empty line to end headers");
        writer.println();
        
        // 7. Send body if present
        if (body != null) {
            Logger.v("Sending request body: '" + body + "'");
            writer.print(body);
        }
        
        writer.flush();
        
        if (writer.checkError()) {
            throw new IOException("Failed to send HTTP request through tunnel");
        }
    }
    
    /**
     * Performs SSL handshake using SSLEngine and tunnel streams.
     */
    private void performSslHandshake(javax.net.ssl.SSLEngine sslEngine, 
                                   java.io.InputStream input, 
                                   java.io.OutputStream output) throws Exception {
        
        // This is a simplified SSL handshake implementation
        // In a real implementation, this would be much more complex
        
        java.nio.ByteBuffer clientBuffer = java.nio.ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        java.nio.ByteBuffer serverBuffer = java.nio.ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        java.nio.ByteBuffer appBuffer = java.nio.ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
        
        javax.net.ssl.SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
        
        while (handshakeStatus != javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED &&
               handshakeStatus != javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            
            switch (handshakeStatus) {
                case NEED_WRAP:
                    clientBuffer.clear();
                    javax.net.ssl.SSLEngineResult wrapResult = sslEngine.wrap(appBuffer, clientBuffer);
                    clientBuffer.flip();
                    output.write(clientBuffer.array(), 0, clientBuffer.limit());
                    output.flush();
                    handshakeStatus = wrapResult.getHandshakeStatus();
                    break;
                    
                case NEED_UNWRAP:
                    serverBuffer.clear();
                    int bytesRead = input.read(serverBuffer.array());
                    if (bytesRead > 0) {
                        serverBuffer.limit(bytesRead);
                        javax.net.ssl.SSLEngineResult unwrapResult = sslEngine.unwrap(serverBuffer, appBuffer);
                        handshakeStatus = unwrapResult.getHandshakeStatus();
                    }
                    break;
                    
                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    handshakeStatus = sslEngine.getHandshakeStatus();
                    break;
                    
                default:
                    throw new IllegalStateException("Unknown handshake status: " + handshakeStatus);
            }
        }
        
        Logger.v("SSL handshake completed successfully");
    }
    
    /**
     * Sends HTTP request through SSL engine.
     */
    private void sendHttpRequestThroughSsl(javax.net.ssl.SSLEngine sslEngine,
                                         java.io.InputStream input,
                                         java.io.OutputStream output,
                                         URL targetUrl,
                                         HttpMethod method,
                                         Map<String, String> headers,
                                         String body) throws Exception {
        
        // Build HTTP request
        StringBuilder requestBuilder = new StringBuilder();
        String path = targetUrl.getPath();
        if (path.isEmpty()) path = "/";
        if (targetUrl.getQuery() != null) path += "?" + targetUrl.getQuery();
        
        requestBuilder.append(method.name()).append(" ").append(path).append(" HTTP/1.1\r\n");
        requestBuilder.append("Host: ").append(targetUrl.getHost()).append("\r\n");
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() != null && header.getValue() != null) {
                requestBuilder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }
        }
        
        if (body != null) {
            requestBuilder.append("Content-Length: ").append(body.length()).append("\r\n");
        }
        
        requestBuilder.append("Connection: close\r\n");
        requestBuilder.append("\r\n");
        
        if (body != null) {
            requestBuilder.append(body);
        }
        
        // Encrypt and send the HTTP request
        byte[] requestBytes = requestBuilder.toString().getBytes("UTF-8");
        java.nio.ByteBuffer plainBuffer = java.nio.ByteBuffer.wrap(requestBytes);
        java.nio.ByteBuffer encryptedBuffer = java.nio.ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        
        javax.net.ssl.SSLEngineResult result = sslEngine.wrap(plainBuffer, encryptedBuffer);
        encryptedBuffer.flip();
        output.write(encryptedBuffer.array(), 0, encryptedBuffer.limit());
        output.flush();
        
        Logger.v("HTTP request sent through SSL engine");
    }
    
    /**
     * Reads HTTP response through SSL engine.
     */
    private HttpResponse readHttpResponseThroughSsl(javax.net.ssl.SSLEngine sslEngine,
                                                  java.io.InputStream input,
                                                  java.io.OutputStream output) throws Exception {
        
        // Read encrypted response and decrypt it
        java.nio.ByteBuffer encryptedBuffer = java.nio.ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        java.nio.ByteBuffer decryptedBuffer = java.nio.ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
        
        int bytesRead = input.read(encryptedBuffer.array());
        if (bytesRead > 0) {
            encryptedBuffer.limit(bytesRead);
            javax.net.ssl.SSLEngineResult result = sslEngine.unwrap(encryptedBuffer, decryptedBuffer);
            
            decryptedBuffer.flip();
            byte[] responseBytes = new byte[decryptedBuffer.limit()];
            decryptedBuffer.get(responseBytes);
            
            // Parse the decrypted HTTP response
            String responseString = new String(responseBytes, "UTF-8");
            Logger.v("Received decrypted HTTP response: " + responseString);
            
            // Parse using our existing parser
            java.io.ByteArrayInputStream responseStream = new java.io.ByteArrayInputStream(responseBytes);
            return mResponseParser.parseHttpResponse(responseStream);
        }
        
        throw new IOException("No response received from origin server");
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
