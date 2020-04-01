package io.split.android.client.impressions;

import io.split.android.client.service.synchronizer.NewSyncManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncImpressionListener implements ImpressionListener {

    private final NewSyncManager mSyncManager;

    public SyncImpressionListener(NewSyncManager syncManager) {
        mSyncManager = checkNotNull(syncManager);
    }

    @Override
    public void log(Impression impression) {
        mSyncManager.pushImpression(impression);
    }

    @Override
    public void close() {
    }
}
