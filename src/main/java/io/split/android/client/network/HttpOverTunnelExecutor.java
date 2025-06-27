package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;

/**
 * Executes HTTP requests through tunnel sockets established by custom tunneling.
 * 
 * Handles both plain sockets (for HTTP origins) and SSL sockets (for HTTPS origins)
 * created by the PlainSocketTunnelEstablisher using the custom tunneling approach.
 * 
 * The socket type is determined by the tunnel establisher based on the origin protocol:
 * - HTTP origins: plain socket after CONNECT
 * - HTTPS origins: SSL socket wrapped after CONNECT
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
     * Executes an HTTP request through the established tunnel socket.
     * 
     * @param tunnelSocket The SSL Socket with tunnel established (connection maintained)
     * @param targetUrl The final destination URL (HTTP or HTTPS)
     * @param method HTTP method for the request
     * @param headers Headers to include in the request
     * @param body Request body (null for GET requests)
     * @return HttpResponse containing the server's response
     * @throws IOException if the request execution fails
     */
    @NonNull
    public HttpResponse executeRequest(
            @NonNull Socket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body) throws IOException {
        
        Logger.v("Executing request through tunnel to: " + targetUrl);
        Logger.v("Socket type: " + tunnelSocket.getClass().getSimpleName());
        
        try {
            // The socket type (plain or SSL) is already determined by PlainSocketTunnelEstablisher
            // based on the origin protocol, so we can handle both uniformly
            return executeHttpRequestThroughTunnel(tunnelSocket, targetUrl, method, headers, body);
            
        } catch (Exception e) {
            Logger.e("Failed to execute request through tunnel: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to execute HTTP request through tunnel to " + targetUrl, e);
        }
    }
    
    /**
     * Executes HTTP request through the tunnel socket.
     * Handles both plain sockets (HTTP origins) and SSL sockets (HTTPS origins).
     */
    @NonNull
    private HttpResponse executeHttpRequestThroughTunnel(
            @NonNull Socket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body) throws IOException {
        
        Logger.v("Sending HTTP request through tunnel socket");
        
        try {
            // Send HTTP request through the tunnel
            sendHttpRequest(tunnelSocket, targetUrl, method, headers, body);
            
            // Read HTTP response
            return readHttpResponse(tunnelSocket);
            
        } catch (Exception e) {
            Logger.e("Failed to execute HTTP request through SSL tunnel: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("HTTP request through SSL tunnel failed", e);
        }
    }
    
    /**
     * Sends the HTTP request through the tunnel socket.
     */
    private void sendHttpRequest(
            @NonNull Socket tunnelSocket,
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
     * Reads HTTP response from the tunnel socket.
     */
    private HttpResponse readHttpResponse(@NonNull Socket tunnelSocket) throws IOException {
        return mResponseParser.parseHttpResponse(tunnelSocket.getInputStream());
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
