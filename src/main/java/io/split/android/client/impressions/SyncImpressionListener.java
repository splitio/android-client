package io.split.android.client.impressions;

import io.split.android.client.service.synchronizer.SyncManager;

import static io.split.android.client.utils.Utils.checkNotNull;

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
