package io.split.android.client.events;

import androidx.annotation.Nullable;

/**
 * Typed metadata for SDK_READY_FROM_CACHE events.
 * <p>
 * Contains information about the cache state when the SDK is ready from cache.
 */
public final class SdkReadyFromCacheMetadata {

    @Nullable
    private final Boolean mFreshInstall;

    @Nullable
    private final Long mLastUpdateTimestamp;

    /**
     * Creates a new SdkReadyFromCacheMetadata instance.
     *
     * @param freshInstall        true if this is a fresh install with no usable cache, or null if not available
     * @param lastUpdateTimestamp the last successful cache timestamp in milliseconds since epoch, or null if not available
     */
    public SdkReadyFromCacheMetadata(@Nullable Boolean freshInstall, @Nullable Long lastUpdateTimestamp) {
        mFreshInstall = freshInstall;
        mLastUpdateTimestamp = lastUpdateTimestamp;
    }

    /**
     * Returns whether this is a fresh install with no usable cache.
     *
     * @return true if fresh install, false otherwise, or null if not available
     */
    @Nullable
    public Boolean isFreshInstall() {
        return mFreshInstall;
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

