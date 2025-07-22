package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;

/**
 * Executes HTTP requests through tunnel sockets established by custom tunneling.
 * <p>
 * This class is responsible for executing HTTP requests through tunnel sockets that have been
 * created by the SSL tunnel establisher using the custom tunneling approach.
 */
class HttpOverTunnelExecutor {

    public static final int HTTP_PORT = 80;
    public static final int HTTPS_PORT = 443;
    public static final int UNSET_PORT = -1;
    private static final String CRLF = "\r\n";

    private final RawHttpResponseParser mResponseParser;

    public HttpOverTunnelExecutor() {
        mResponseParser = new RawHttpResponseParser();
    }

    @NonNull
    HttpResponse executeRequest(
            @NonNull Socket tunnelSocket,
            @NonNull URL targetUrl,
            @NonNull HttpMethod method,
            @NonNull Map<String, String> headers,
            @Nullable String body,
            @Nullable Certificate[] serverCertificates) throws IOException {

        Logger.v("Executing request through tunnel to: " + targetUrl);

        try {
            sendHttpRequest(tunnelSocket, targetUrl, method, headers, body);

            return readHttpResponse(tunnelSocket, serverCertificates);
        } catch (SocketException e) {
            // Let socket-related IOExceptions pass through unwrapped
            // This ensures consistent behavior with non-proxy flows
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions in IOException
            Logger.e("Failed to execute request through tunnel: " + e.getMessage());
            throw new IOException("Failed to execute HTTP request through tunnel to " + targetUrl, e);
        }
    }

    @NonNull
    HttpStreamResponse executeStreamRequest(@NonNull Socket finalSocket,
                                            @Nullable Socket tunnelSocket,
                                            @Nullable Socket originSocket,
                                            @NonNull URL targetUrl,
                                            @NonNull HttpMethod method,
                                            @NonNull Map<String, String> headers,
                                            @Nullable Certificate[] serverCertificates) throws IOException {
        Logger.v("Executing stream request through tunnel to: " + targetUrl);

        try {
            sendHttpRequest(finalSocket, targetUrl, method, headers, null);
            return readHttpStreamResponse(finalSocket, originSocket);
        } catch (SocketException e) {
            // Let socket-related IOExceptions pass through unwrapped
            // This ensures consistent behavior with non-proxy flows
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions in IOException
            Logger.e("Failed to execute stream request through tunnel: " + e.getMessage());
            throw new IOException("Failed to execute HTTP stream request through tunnel to " + targetUrl, e);
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
     *
     * @param tunnelSocket       The socket to read from
     * @param serverCertificates The server certificates to include in the response
     * @return HttpResponse with server certificates
     */
    private HttpResponse readHttpResponse(@NonNull Socket tunnelSocket, @Nullable Certificate[] serverCertificates) throws IOException {
        return mResponseParser.parseHttpResponse(tunnelSocket.getInputStream(), serverCertificates);
    }

    private HttpStreamResponse readHttpStreamResponse(@NonNull Socket tunnelSocket) throws IOException {
        return readHttpStreamResponse(tunnelSocket, null);
    }

    private HttpStreamResponse readHttpStreamResponse(@NonNull Socket tunnelSocket, @Nullable Socket originSocket) throws IOException {
        return mResponseParser.parseHttpStreamResponse(tunnelSocket.getInputStream(), tunnelSocket, originSocket);
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
