package io.split.android.client.events;

import androidx.annotation.Nullable;

/**
 * Typed metadata for SDK_READY and SDK_READY_FROM_CACHE events.
 * <p>
 * Contains information about the cache state when the SDK becomes ready.
 */
public final class SdkReadyMetadata {

    @Nullable
    private final Boolean mInitialCacheLoad;

    @Nullable
    private final Long mLastUpdateTimestamp;

    /**
     * Creates a new SdkReadyMetadata instance.
     *
     * @param initialCacheLoad    true if this is an initial cache load with no usable cache, or null if not available
     * @param lastUpdateTimestamp the last successful cache timestamp in milliseconds since epoch, or null if not available
     */
    public SdkReadyMetadata(@Nullable Boolean initialCacheLoad, @Nullable Long lastUpdateTimestamp) {
        mInitialCacheLoad = initialCacheLoad;
        mLastUpdateTimestamp = lastUpdateTimestamp;
    }

    /**
     * Returns whether this is an initial cache load with no usable cache.
     * <p>
     * This is true when the SDK starts without any prior cached data (fresh install),
     * meaning data was fetched from the server for the first time.
     *
     * @return true if initial cache load, false otherwise, or null if not available
     */
    @Nullable
    public Boolean isInitialCacheLoad() {
        return mInitialCacheLoad;
    }

    /**
     * Returns the last successful cache timestamp in milliseconds since epoch.
     *
     * @return the timestamp, or null if not available
     */
    @Nullable
    public Long getLastUpdateTimestamp() {
        return mLastUpdateTimestamp;
    }
}

