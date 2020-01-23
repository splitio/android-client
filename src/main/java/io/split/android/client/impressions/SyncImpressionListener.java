package io.split.android.client.impressions;

import io.split.android.client.service.SyncManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncImpressionListener implements ImpressionListener {

    private final SyncManager mSyncManager;

    public SyncImpressionListener(SyncManager syncManager) {
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
