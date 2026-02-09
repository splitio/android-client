package io.split.android.client.service.sseclient.spi;

/**
 * Interface for recording streaming-related telemetry.
 * Implementations should bridge to the host application's telemetry system.
 */
public interface StreamingTelemetry {

    /**
     * Records a sync latency measurement for token operations.
     *
     * @param latencyMillis the latency in milliseconds
     */
    void recordTokenSyncLatency(long latencyMillis);

    /**
     * Records a successful token sync operation.
     *
     * @param timestamp the timestamp of the sync
     */
    void recordTokenSuccessfulSync(long timestamp);

    /**
     * Records a token sync error.
     *
     * @param httpStatus the HTTP status code
     */
    void recordTokenSyncError(Integer httpStatus);

    /**
     * Records an authentication rejection.
     */
    void recordAuthRejections();

    /**
     * Records a token refresh.
     */
    void recordTokenRefreshes();

    /**
     * Records a token refresh streaming event.
     *
     * @param expirationTime the token expiration time
     * @param timestamp the timestamp
     */
    void recordTokenRefreshEvent(long expirationTime, long timestamp);

    /**
     * Records a sync mode update (streaming enabled).
     *
     * @param streaming true if streaming mode, false if polling
     * @param timestamp the timestamp
     */
    void recordSyncModeUpdate(boolean streaming, long timestamp);

    /**
     * Records an SSE connection error.
     *
     * @param retryable true if the error is retryable
     * @param timestamp the timestamp
     */
    void recordConnectionError(boolean retryable, long timestamp);

    /**
     * Records an Ably error.
     *
     * @param errorCode the error code
     * @param timestamp the timestamp
     */
    void recordAblyError(int errorCode, long timestamp);

    /**
     * Records an occupancy event on the primary channel.
     *
     * @param publisherCount the publisher count
     * @param timestamp the timestamp
     */
    void recordOccupancyPri(int publisherCount, long timestamp);

    /**
     * Records an occupancy event on the secondary channel.
     *
     * @param publisherCount the publisher count
     * @param timestamp the timestamp
     */
    void recordOccupancySec(int publisherCount, long timestamp);

    /**
     * Records a streaming status change.
     *
     * @param status the new status (ENABLED, PAUSED, DISABLED)
     * @param timestamp the timestamp
     */
    void recordStreamingStatus(StreamingStatus status, long timestamp);

    /**
     * Streaming status values.
     */
    enum StreamingStatus {
        ENABLED,
        PAUSED,
        DISABLED
    }
}
