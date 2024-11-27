package io.split.android.client.service.synchronizer;

import androidx.annotation.VisibleForTesting;

import io.split.android.client.SplitClientConfig;

class RolloutCacheManagerConfig {

    private final long mCacheExpirationInDays;
    private final boolean mClearOnInit;

    @VisibleForTesting
    RolloutCacheManagerConfig(long cacheExpirationInDays, boolean clearOnInit) {
        mCacheExpirationInDays = cacheExpirationInDays;
        mClearOnInit = clearOnInit;
    }

    public static RolloutCacheManagerConfig from(SplitClientConfig splitClientConfig) {
        return new RolloutCacheManagerConfig(splitClientConfig.cacheExpirationInSeconds(), false); // TODO
    }

    public long getCacheExpirationInDays() {
        return mCacheExpirationInDays;
    }

    public boolean isClearOnInit() {
        return mClearOnInit;
    }
}
