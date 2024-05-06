package io.split.android.client.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;

import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.utils.logger.Logger;

public class SyncImpressionListener implements ImpressionListener {

    private final SyncManager mSyncManager;
    private final ExecutorService mExecutorService;

    public SyncImpressionListener(@NonNull SyncManager syncManager,
                                  @NonNull ExecutorService executorService) {
        mSyncManager = checkNotNull(syncManager);
        mExecutorService = checkNotNull(executorService);
    }

    @Override
    public void log(Impression impression) {
        try {
            mExecutorService.submit(new ImpressionLoggingTask(mSyncManager, impression));
        } catch (Exception ex) {
            Logger.w("Error submitting impression logging task: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void close() {
    }
}
