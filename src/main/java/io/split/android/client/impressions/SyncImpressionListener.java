package io.split.android.client.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.utils.logger.Logger;

public class SyncImpressionListener implements ImpressionListener {

    private final SyncManager mSyncManager;
    private final SplitTaskExecutor mSplitTaskExecutor;

    public SyncImpressionListener(@NonNull SyncManager syncManager,
                                  @NonNull SplitTaskExecutor splitTaskExecutor) {
        mSyncManager = checkNotNull(syncManager);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
    }

    @Override
    public void log(Impression impression) {
        try {
            mSplitTaskExecutor.submit(new ImpressionLoggingTask(mSyncManager, impression), null);
        } catch (Exception ex) {
            Logger.w("Error submitting impression logging task: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void close() {
    }
}
