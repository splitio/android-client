package io.split.android.client.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.utils.logger.Logger;

class ImpressionLoggingTask implements SplitTask {

    private final SyncManager mSyncManager;
    private final Impression mImpression;

    ImpressionLoggingTask(@NonNull SyncManager syncManager,
                          Impression impression) {
        mSyncManager = checkNotNull(syncManager);
        mImpression = impression;
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mSyncManager.pushImpression(mImpression);
        } catch (Throwable t) {
            Logger.e("An error occurred logging impression: " + t.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK);
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
