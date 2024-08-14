package io.split.android.client.service.synchronizer;

import java.util.concurrent.TimeUnit;

import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.utils.logger.Logger;

class RolloutCacheManagerImpl implements RolloutCacheManager {

    private final LastUpdateTimestampProvider mTimestampProvider;
    private final FeatureFlagsSynchronizer mFeatureFlagsSynchronizer;
    private final MySegmentsSynchronizerRegistry.Tasks mMySegmentsSynchronizerRegistry;
    private final boolean mForceCacheExpiration;
    private final long mCacheExpirationPeriod;

    RolloutCacheManagerImpl(LastUpdateTimestampProvider timestampProvider,
                        FeatureFlagsSynchronizer featureFlagsSynchronizer,
                        MySegmentsSynchronizerRegistry.Tasks mySegmentsSynchronizerRegistry,
                        boolean forceCacheExpiration,
                        long cacheExpirationPeriod) {
        mTimestampProvider = timestampProvider;
        mFeatureFlagsSynchronizer = featureFlagsSynchronizer;
        mMySegmentsSynchronizerRegistry = mySegmentsSynchronizerRegistry;
        mForceCacheExpiration = forceCacheExpiration;
        mCacheExpirationPeriod = cacheExpirationPeriod;
    }

    @Override
    public void validateCache() {
        long lastUpdateTimestamp = mTimestampProvider.getLastUpdateTimestamp();
        long currentTime = System.currentTimeMillis() / 1000;
        long elapsedTime = currentTime - lastUpdateTimestamp;

        if (mForceCacheExpiration || TimeUnit.MILLISECONDS.toSeconds(elapsedTime) > mCacheExpirationPeriod) {
            if (mForceCacheExpiration) {
                Logger.v("Forcing cache expiration");
            } else {
                Logger.v("Cache expired due to time");
            }
            clearCache();
        }
    }

    private void clearCache() {
        mFeatureFlagsSynchronizer.expireCache();
        mMySegmentsSynchronizerRegistry.expireCache();
    }
}

interface RolloutCacheManager {

    void validateCache();
}

