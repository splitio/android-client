package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;

import java.net.URI;
import java.util.Map;

/**
 * Generic Server-Sent Events (SSE) client interface.
 * Connects to an SSE endpoint and delivers raw events via an {@link EventHandler}.
 * <p>
 * This client is protocol-aware only — it understands SSE framing
 * (event, data, id fields) but has no knowledge of application-level
 * message semantics.
 */
public interface EventSourceClient {

    int CONNECTING = 0;
    int CONNECTED = 1;
    int DISCONNECTED = 2;

    /**
     * @return the current connection status.
     */
    int status();

    /**
     * Disconnects the SSE stream. Safe to call from any thread.
     * If called while {@link #connect} is blocking, the read loop
     * will be interrupted and {@link EventHandler#onError} will NOT fire.
     */
    void disconnect();

    /**
     * Opens an SSE connection to the given URI and blocks while reading events.
     * Events are delivered to the supplied {@link EventHandler}.
     * <p>
     * This method returns only when the connection is closed (either by
     * calling {@link #disconnect()}, by a transport error, or when the
     * server closes the stream).
     *
     * @param url     fully-built URI to connect to
     * @param handler callback for SSE lifecycle events
     */
    void connect(@NonNull URI url, @NonNull EventHandler handler);

    /**
     * Callback interface for SSE lifecycle events.
     */
    interface EventHandler {

        /**
         * Called when the HTTP connection succeeds and the event stream is open.
         */
        void onOpen();

        /**
         * Called for each complete SSE event parsed from the stream.
         * Keepalive events are included — the handler decides what to do with them.
         *
         * @param event the parsed SSE field→value map
         *              (typically contains "event", "data", and/or "id" keys)
         */
        void onMessage(@NonNull Map<String, String> event);

        /**
         * Called when the connection ends unexpectedly (NOT via {@link #disconnect()}).
         *
         * @param retryable {@code true} if the error suggests a retry is reasonable
         */
        void onError(boolean retryable);
    }
}
