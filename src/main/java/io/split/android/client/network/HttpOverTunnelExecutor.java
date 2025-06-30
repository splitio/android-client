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
 * This class is responsible for executing HTTP requests through tunnel sockets that have been
 * created by the SSL tunnel establisher using the custom tunneling approach.
 * 
 * The socket type is determined by the tunnel establisher based on the origin protocol:
 * - HTTP origins: plain socket after CONNECT
 * - HTTPS origins: SSL socket wrapped after CONNECT
 */
class HttpOverTunnelExecutor {

    public static final int HTTP_PORT = 80;
    public static final int HTTPS_PORT = 443;
    public static final int UNSET_PORT = -1;
    private static final String CRLF = "\r\n";

    private final HttpResponseParser mResponseParser;

    public HttpOverTunnelExecutor() {
        mResponseParser = new HttpResponseParser();
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
        
        try {
            sendHttpRequest(tunnelSocket, targetUrl, method, headers, body);

            return readHttpResponse(tunnelSocket);
        } catch (Exception e) {
            Logger.e("Failed to execute request through tunnel: " + e.getMessage());
            throw new IOException("Failed to execute HTTP request through tunnel to " + targetUrl, e);
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
        writer.write(requestLine + CRLF);
        
        // 2. Send Host header (required for HTTP/1.1)
        String host = targetUrl.getHost();
        int port = getTargetPort(targetUrl);
        
        // Add port to Host header if it's not the default port for the protocol
        if (!isIsDefaultPort(targetUrl, port)) {
            host += ":" + port;
        }

        Logger.v("Sending Host header: 'Host: " + host + "'");
        writer.write("Host: " + host + CRLF);

        // 3. Send custom headers (excluding Host and Content-Length)
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() != null && header.getValue() != null &&
                !"content-length".equalsIgnoreCase(header.getKey()) &&
                !"host".equalsIgnoreCase(header.getKey())) {
                String headerLine = header.getKey() + ": " + header.getValue();
                Logger.v("Sending header: '" + headerLine + "'");
                writer.write(headerLine + CRLF);
            }
        }

        // 4. Send Content-Length header if body is present
        if (body != null) {
            String contentLengthHeader = "Content-Length: " + body.getBytes("UTF-8").length;
            writer.write(contentLengthHeader + CRLF);
        }
        
        // 5. Send Connection: close to ensure response completion
        writer.write("Connection: close" + CRLF);
        
        // 6. End headers with empty line
        writer.write(CRLF);
        
        // 7. Send body if present
        if (body != null) {
            Logger.v("Sending request body: '" + body + "'");
            writer.write(body);
        }
        
        writer.flush();
        
        if (writer.checkError()) {
            throw new IOException("Failed to send HTTP request through tunnel");
        }
    }

    private static boolean isIsDefaultPort(@NonNull URL targetUrl, int port) {
        return ("http".equalsIgnoreCase(targetUrl.getProtocol()) && port == HTTP_PORT) ||
                ("https".equalsIgnoreCase(targetUrl.getProtocol()) && port == HTTPS_PORT);
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
        if (port == UNSET_PORT) {
            if ("https".equalsIgnoreCase(targetUrl.getProtocol())) {
                return HTTPS_PORT;
            } else if ("http".equalsIgnoreCase(targetUrl.getProtocol())) {
                return HTTP_PORT;
            }
        }
        return port;
    }
}
