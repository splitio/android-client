package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.split.android.client.utils.logger.Logger;

/**
 * Parses raw HTTP protocol responses from socket input streams.
 * This is different from the existing HttpResponseParser<T> interface which parses
 * JSON response data into domain objects. This class handles the low-level HTTP
 * protocol parsing (status line, headers, body) from socket streams.
 */
class HttpResponseParser {

    /**
     * Parses a raw HTTP response from an input stream.
     * 
     * @param inputStream The input stream containing the raw HTTP response
     * @return HttpResponse containing the parsed status code and response data
     * @throws IOException if parsing fails or the response is malformed
     */
    @NonNull
    public HttpResponse parseHttpResponse(@NonNull InputStream inputStream) throws IOException {
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        // 1. Read and parse status line
        String statusLine = reader.readLine();
        if (statusLine == null) {
            throw new IOException("No HTTP response received from server");
        }
        
        Logger.v("Parsing HTTP status line: " + statusLine);
        int statusCode = parseStatusCode(statusLine);
        
        // 2. Read and parse response headers
        int contentLength = 0;
        boolean isChunked = false;
        boolean connectionClose = false;
        String headerLine;
        
        while ((headerLine = reader.readLine()) != null && !headerLine.trim().isEmpty()) {
            Logger.v("Parsing HTTP header: " + headerLine);
            
            String lowerHeader = headerLine.toLowerCase();
            if (lowerHeader.startsWith("content-length:")) {
                String lengthStr = headerLine.substring("content-length:".length()).trim();
                try {
                    contentLength = Integer.parseInt(lengthStr);
                } catch (NumberFormatException e) {
                    Logger.w("Invalid Content-Length header: " + headerLine);
                }
            } else if (lowerHeader.startsWith("transfer-encoding:") && 
                       lowerHeader.contains("chunked")) {
                isChunked = true;
            } else if (lowerHeader.startsWith("connection:") && 
                       lowerHeader.contains("close")) {
                connectionClose = true;
            }
        }
        
        // 3. Read response body based on encoding type
        String responseBody = null;
        if (isChunked) {
            responseBody = readChunkedBody(reader);
        } else if (contentLength > 0) {
            responseBody = readFixedLengthBody(reader, contentLength);
        } else if (connectionClose) {
            responseBody = readUntilClose(reader);
        }
        
        Logger.v("Parsed HTTP response: status=" + statusCode + 
                 ", bodyLength=" + (responseBody != null ? responseBody.length() : 0));
        
        // 4. Create and return HttpResponse
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            return new HttpResponseImpl(statusCode, responseBody);
        } else {
            return new HttpResponseImpl(statusCode);
        }
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
     * Reads a fixed-length response body based on Content-Length header.
     */
    @NonNull
    private String readFixedLengthBody(@NonNull BufferedReader reader, int contentLength) throws IOException {
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;
        
        while (totalRead < contentLength) {
            int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
            if (read == -1) {
                break; // End of stream
            }
            totalRead += read;
        }
        
        return new String(bodyChars, 0, totalRead);
    }
    
    /**
     * Reads response body until connection closes (for Connection: close).
     */
    @NonNull
    private String readUntilClose(@NonNull BufferedReader reader) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        String line;
        boolean firstLine = true;
        
        while ((line = reader.readLine()) != null) {
            if (!firstLine) {
                bodyBuilder.append("\n");
            }
            bodyBuilder.append(line);
            firstLine = false;
        }
        
        return bodyBuilder.toString();
    }
    
    /**
     * Reads chunked response body (simplified implementation).
     * Note: This is a basic implementation that may not handle all chunked encoding edge cases.
     */
    @NonNull
    private String readChunkedBody(@NonNull BufferedReader reader) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        
        try {
            String chunkSizeLine;
            while ((chunkSizeLine = reader.readLine()) != null) {
                // Parse chunk size (hex format)
                int chunkSize;
                try {
                    chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
                } catch (NumberFormatException e) {
                    Logger.w("Invalid chunk size: " + chunkSizeLine);
                    break;
                }
                
                // If chunk size is 0, we've reached the end
                if (chunkSize == 0) {
                    // Read final CRLF and any trailing headers
                    reader.readLine();
                    break;
                }
                
                // Read chunk data
                char[] chunkData = new char[chunkSize];
                int totalRead = 0;
                while (totalRead < chunkSize) {
                    int read = reader.read(chunkData, totalRead, chunkSize - totalRead);
                    if (read == -1) {
                        break;
                    }
                    totalRead += read;
                }
                
                bodyBuilder.append(chunkData, 0, totalRead);
                
                // Read trailing CRLF after chunk data
                reader.readLine();
            }
        } catch (IOException e) {
            Logger.w("Error reading chunked body, returning partial data: " + e.getMessage());
        }
        
        return bodyBuilder.toString();
    }
}
