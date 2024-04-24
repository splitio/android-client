package io.split.android.client.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.split.android.client.service.synchronizer.SyncManager;

public class SyncImpressionListenerBis implements ImpressionListener {

    private final SyncManager mSyncManager;
    private final ExecutorService mExecutorService;

    public SyncImpressionListenerBis(@NonNull SyncManager syncManager) {
        this(syncManager, new ThreadPoolExecutor(1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(4000),
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    SyncImpressionListenerBis(@NonNull SyncManager syncManager, ThreadPoolExecutor executorService) {
        mSyncManager = checkNotNull(syncManager);
        mExecutorService = executorService;
    }

    @Override
    public void log(Impression impression) {
        ImpressionLoggingTask task = new ImpressionLoggingTask(mSyncManager, impression);
        mExecutorService.submit(task);
    }

    @Override
    public void close() {

    }

    private static class ImpressionLoggingTask implements Runnable {
        private final SyncManager mSyncManager;
        private final Impression mImpression;

        public ImpressionLoggingTask(SyncManager syncManager, Impression impression) {
            mSyncManager = syncManager;
            mImpression = impression;
        }

        @Override
        public void run() {
            mSyncManager.pushImpression(mImpression);
        }
    }
}
