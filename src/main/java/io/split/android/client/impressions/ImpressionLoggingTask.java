package io.split.android.client.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.utils.logger.Logger;

class ImpressionLoggingTask implements Runnable {

    private final SyncManager mSyncManager;
    private final Impression mImpression;

    ImpressionLoggingTask(@NonNull SyncManager syncManager,
                          Impression impression) {
        mSyncManager = checkNotNull(syncManager);
        mImpression = impression;
    }

    @Override
    public void run() {
        try {
            mSyncManager.pushImpression(mImpression);
        } catch (Throwable t) {
            Logger.v("An error occurred logging impression: " + t.getLocalizedMessage());
        }
    }
}
