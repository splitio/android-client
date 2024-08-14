package io.split.android.client.service.synchronizer;

import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.utils.logger.Logger;

class RolloutCacheManagerImpl implements RolloutCacheManager {

    private final FeatureFlagsSynchronizer mFeatureFlagsSynchronizer;
    private final MySegmentsSynchronizerRegistry.Tasks mMySegmentsSynchronizerRegistry;

    RolloutCacheManagerImpl(FeatureFlagsSynchronizer featureFlagsSynchronizer,
                            MySegmentsSynchronizerRegistry.Tasks mySegmentsSynchronizerRegistry) {
        mFeatureFlagsSynchronizer = featureFlagsSynchronizer;
        mMySegmentsSynchronizerRegistry = mySegmentsSynchronizerRegistry;
    }

    @Override
    public void clearRolloutCaches() {
        Logger.v("Forcing cache expiration");

        mFeatureFlagsSynchronizer.expireCache();
        mMySegmentsSynchronizerRegistry.expireCache();
    }
}

interface RolloutCacheManager {

    void clearRolloutCaches();
}

