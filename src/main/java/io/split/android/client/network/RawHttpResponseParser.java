package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.split.android.client.utils.logger.Logger;

/**
 * Parses raw HTTP protocol responses from socket input streams.
 * Handles the HTTP protocol parsing (status line, headers, body) from socket streams.
 */
class RawHttpResponseParser {

    /**
     * Parses a raw HTTP response from an input stream.
     *
     * @param inputStream        The input stream containing the raw HTTP response
     * @param serverCertificates The server certificates to include in the response
     * @return HttpResponse containing the parsed status code, headers, and response data
     * @throws IOException if parsing fails or the response is malformed
     */
    @NonNull
    HttpResponse parseHttpResponse(@NonNull InputStream inputStream, Certificate[] serverCertificates) throws IOException {
        // 1. Read and parse status line
        String statusLine = readLineFromStream(inputStream);
        if (statusLine == null) {
            throw new IOException("No HTTP response received from server");
        }
        
        int statusCode = parseStatusCode(statusLine);
        
        // 2. Read and parse response headers directly
        ParsedResponseHeaders responseHeaders = parseHeadersDirectly(inputStream);
        
        // 3. Determine charset from Content-Type header
        Charset bodyCharset = extractCharsetFromContentType(responseHeaders.mContentType);

        // 4. Read response body using the same InputStream
        String responseBody = readResponseBody(inputStream, responseHeaders.mIsChunked, bodyCharset, responseHeaders.mContentLength, responseHeaders.mConnectionClose);
        
        // 5. Create and return HttpResponse
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            return new HttpResponseImpl(statusCode, responseBody, serverCertificates);
        } else {
            return new HttpResponseImpl(statusCode, serverCertificates);
        }
    }

    @NonNull
    HttpStreamResponse parseHttpStreamResponse(@NonNull InputStream inputStream, 
                                              @Nullable Socket tunnelSocket, 
                                              @Nullable Socket originSocket) throws IOException {
        // 1. Read and parse status line
        String statusLine = readLineFromStream(inputStream);
        if (statusLine == null) {
            throw new IOException("No HTTP response received from server");
        }

        int statusCode = parseStatusCode(statusLine);

        // 2. Read and parse response headers directly
        ParsedResponseHeaders responseHeaders = parseHeadersDirectly(inputStream);

        // 3. Determine charset from Content-Type header
        Charset bodyCharset = extractCharsetFromContentType(responseHeaders.mContentType);

        return HttpStreamResponseImpl.createFromTunnelSocket(statusCode,
                new BufferedReader(new InputStreamReader(inputStream, bodyCharset)),
                tunnelSocket,
                originSocket);
    }

    @NonNull
    private ParsedResponseHeaders parseHeadersDirectly(@NonNull InputStream inputStream) throws IOException {
        int contentLength = -1;
        boolean isChunked = false;
        boolean connectionClose = false;
        String contentType = null;
        String headerLine;
        
        while ((headerLine = readLineFromStream(inputStream)) != null && !headerLine.trim().isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();

                String lowerHeaderName = headerName.toLowerCase(Locale.US);
                if ("content-length".equals(lowerHeaderName)) {
                    try {
                        contentLength = Integer.parseInt(headerValue);
                    } catch (NumberFormatException e) {
                        Logger.w("Invalid Content-Length header: " + headerLine);
                    }
                } else if ("transfer-encoding".equals(lowerHeaderName) && headerValue.toLowerCase(Locale.US).contains("chunked")) {
                    isChunked = true;
                } else if ("connection".equals(lowerHeaderName) && headerValue.toLowerCase(Locale.US).contains("close")) {
                    connectionClose = true;
                } else if ("content-type".equals(lowerHeaderName)) {
                    contentType = headerValue;
                }
            }
        }
        return new ParsedResponseHeaders(contentLength, isChunked, connectionClose, contentType);
    }

    @Nullable
    private String readResponseBody(@NonNull InputStream inputStream, boolean isChunked, Charset bodyCharset, int contentLength, boolean connectionClose) throws IOException {
        String responseBody = null;
        if (isChunked) {
            responseBody = readChunkedBodyWithCharset(inputStream, bodyCharset);
        } else if (contentLength > 0) {
            responseBody = readFixedLengthBodyWithCharset(inputStream, contentLength, bodyCharset);
        } else if (connectionClose) {
            responseBody = readUntilCloseWithCharset(inputStream, bodyCharset);
        }
        return responseBody;
    }

    /**
     * Parses the HTTP status code from the status line.
     */
    private int parseStatusCode(@NonNull String statusLine) throws IOException {
        // Status line format: "HTTP/1.1 200 OK" or "HTTP/1.0 404 Not Found"
        String[] parts = statusLine.split(" ");
        if (parts.length < 2) {
            throw new IOException("Invalid HTTP status line: " + statusLine);
        }

        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid HTTP status code in line: " + statusLine, e);
        }
    }

    /**
     * Extracts charset from Content-Type header, defaulting to UTF-8.
     */
    private Charset extractCharsetFromContentType(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }

        // Pattern to match charset=value in Content-Type header
        Pattern charsetPattern = Pattern.compile("charset\\s*=\\s*([^\\s;]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = charsetPattern.matcher(contentType);

        if (matcher.find()) {
            String charsetName = matcher.group(1).replaceAll("[\"']", ""); // Remove quotes
            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                Logger.w("Unsupported charset: " + charsetName + ", using UTF-8");
            }
        }

        return StandardCharsets.UTF_8;
    }

    private String readChunkedBodyWithCharset(InputStream inputStream, Charset charset) throws IOException {
        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();

        while (true) {
            // Read chunk size line
            String chunkSizeLine = readLineFromStream(inputStream);
            if (chunkSizeLine == null) {
                throw new IOException("Unexpected EOF while reading chunk size");
            }

            // Parse chunk size (ignore extensions after semicolon)
            int semicolonIndex = chunkSizeLine.indexOf(';');
            String sizeStr = semicolonIndex >= 0 ? chunkSizeLine.substring(0, semicolonIndex).trim() : chunkSizeLine.trim();

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeStr, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size: " + chunkSizeLine, e);
            }

            if (chunkSize < 0) {
                throw new IOException("Negative chunk size: " + chunkSize);
            }

            // If chunk size is 0, we've reached the end
            if (chunkSize == 0) {
                // Read trailing headers until empty line
                String trailerLine;
                while ((trailerLine = readLineFromStream(inputStream)) != null && !trailerLine.trim().isEmpty()) {
                    // no-op
                }
                break;
            }

            // Read chunk data (exact byte count)
            byte[] chunkData = new byte[chunkSize];
            int totalRead = 0;
            while (totalRead < chunkSize) {
                int read = inputStream.read(chunkData, totalRead, chunkSize - totalRead);
                if (read == -1) {
                    throw new IOException("Unexpected EOF while reading chunk data");
                }
                totalRead += read;
            }

            bodyBytes.write(chunkData);

            // Read trailing CRLF after chunk data
            int c1 = inputStream.read();
            int c2 = inputStream.read();
            if (c1 != '\r' || c2 != '\n') {
                throw new IOException("Expected CRLF after chunk data, got: " + (char) c1 + (char) c2);
            }
        }

        return new String(bodyBytes.toByteArray(), charset);
    }

    private String readFixedLengthBodyWithCharset(InputStream inputStream, int contentLength, Charset charset) throws IOException {
        byte[] bodyBytes = new byte[contentLength];
        int totalRead = 0;

        while (totalRead < contentLength) {
            int read = inputStream.read(bodyBytes, totalRead, contentLength - totalRead);
            if (read == -1) {
                throw new IOException("Unexpected EOF while reading fixed-length body");
            }
            totalRead += read;
        }

        return new String(bodyBytes, charset);
    }

    private String readUntilCloseWithCharset(InputStream inputStream, Charset charset) throws IOException {
        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            bodyBytes.write(buffer, 0, bytesRead);
        }

        return new String(bodyBytes.toByteArray(), charset);
    }

    private String readLineFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
        int b;
        boolean foundCR = false;

        while ((b = inputStream.read()) != -1) {
            if (b == '\r') {
                foundCR = true;
            } else if (b == '\n' && foundCR) {
                break;
            } else if (foundCR) {
                // CR not followed by LF, add the CR to output
                lineBytes.write('\r');
                lineBytes.write(b);
                foundCR = false;
            } else {
                lineBytes.write(b);
            }
        }

        if (b == -1 && lineBytes.size() == 0) {
            return null; // EOF
        }

        return new String(lineBytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static class ParsedResponseHeaders {
        final int mContentLength;
        final boolean mIsChunked;
        final boolean mConnectionClose;
        final String mContentType;

        ParsedResponseHeaders(int contentLength, boolean isChunked, boolean connectionClose, String contentType) {
            mContentLength = contentLength;
            mIsChunked = isChunked;
            mConnectionClose = connectionClose;
            mContentType = contentType;
        }
    }
}
