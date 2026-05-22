package io.split.android.client.service.sseclient.spi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * Interface for SSE streaming transport. Implementations should provide
 * the ability to open streaming connections and return response objects
 * that expose buffered readers for line-by-line reading.
 */
public interface StreamingTransport {

    /**
     * Opens a streaming connection to the given URI.
     *
     * @param uri the target URI
     * @return a StreamingConnection that can be used to execute the request
     */
    @NonNull
    StreamingConnection connect(@NonNull URI uri);

    /**
     * Represents a streaming connection that can be executed to obtain a response.
     */
    interface StreamingConnection {

        /**
         * Executes the streaming request and returns the response.
         *
         * @return the streaming response
         * @throws StreamingTransportException if an error occurs during the request
         */
        @NonNull
        StreamingResponse execute() throws StreamingTransportException;

        /**
         * Closes this connection and releases associated resources.
         */
        void close();
    }

    /**
     * Represents the response from a streaming connection.
     */
    interface StreamingResponse extends Closeable {

        /**
         * @return true if the connection was successful (HTTP 2xx)
         */
        boolean isSuccess();

        /**
         * @return the HTTP status code
         */
        int getHttpStatus();

        /**
         * @return true if the error is client-related (4xx except 408)
         */
        boolean isClientRelatedError();

        /**
         * @return the buffered reader for reading the stream, or null if not available
         */
        @Nullable
        BufferedReader getBufferedReader();
    }

    /**
     * Exception thrown by streaming transport operations.
     */
    class StreamingTransportException extends Exception {

        @Nullable
        private final Integer mStatusCode;

        public StreamingTransportException(String message) {
            super(message);
            mStatusCode = null;
        }

        public StreamingTransportException(String message, Throwable cause) {
            super(message, cause);
            mStatusCode = null;
        }

        public StreamingTransportException(String message, int statusCode) {
            super(message);
            mStatusCode = statusCode;
        }

        public StreamingTransportException(String message, Throwable cause, int statusCode) {
            super(message, cause);
            mStatusCode = statusCode;
        }

        /**
         * @return the HTTP status code if available, null otherwise
         */
        @Nullable
        public Integer getStatusCode() {
            return mStatusCode;
        }
    }
}
