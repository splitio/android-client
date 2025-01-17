package io.split.android.client.service.impressions.strategy;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.observer.ImpressionsObserver;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;

class OptimizedTracker implements PeriodicTracker {

    private final ImpressionsObserver mImpressionsObserver;
    private final RecorderSyncHelper<KeyImpression> mImpressionsSyncHelper;
    private final SplitTaskExecutor mTaskExecutor;
    private final ImpressionsTaskFactory mImpressionsTaskFactory;

    private final RetryBackoffCounterTimer mRetryTimer;
    private final int mImpressionsRefreshRate;
    private String mImpressionsRecorderTaskId;
    private final AtomicBoolean mTrackingIsEnabled;
    private final AtomicBoolean mIsSynchronizing = new AtomicBoolean(true);
    /**
     * @noinspection FieldCanBeLocal
     */
    private final SplitTaskExecutionListener mTaskExecutionListener = new SplitTaskExecutionListener() {
        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            // this listener intercepts impressions recording task
            if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
                if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                    mIsSynchronizing.compareAndSet(true, false);
                    stopPeriodicRecording();
                }
            }
        }
    };

    OptimizedTracker(@NonNull ImpressionsObserver impressionsObserver,
                     @NonNull RecorderSyncHelper<KeyImpression> impressionsSyncHelper,
                     @NonNull SplitTaskExecutor taskExecutor,
                     @NonNull ImpressionsTaskFactory taskFactory,

                     @NonNull RetryBackoffCounterTimer impressionsRetryTimer,
                     int impressionsRefreshRate,
                     boolean isTrackingEnabled) {
        mImpressionsObserver = checkNotNull(impressionsObserver);
        mImpressionsSyncHelper = checkNotNull(impressionsSyncHelper);
        mImpressionsSyncHelper.addListener(mTaskExecutionListener);
        mTaskExecutor = checkNotNull(taskExecutor);
        mImpressionsTaskFactory = checkNotNull(taskFactory);

        mRetryTimer = checkNotNull(impressionsRetryTimer);
        mImpressionsRefreshRate = impressionsRefreshRate;
        mTrackingIsEnabled = new AtomicBoolean(isTrackingEnabled);
    }

    @Override
    public void flush() {
        flushImpressions();
    }

    @Override
    public void startPeriodicRecording() {
        if (mIsSynchronizing.get()) {
            scheduleImpressionsRecorderTask();
        }
    }

    @Override
    public void stopPeriodicRecording() {
        mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
        mImpressionsObserver.persist();
    }

    @Override
    public void enableTracking(boolean enable) {
        mTrackingIsEnabled.set(enable);
    }

    private void flushImpressions() {
        mRetryTimer.setTask(
                mImpressionsTaskFactory.createImpressionsRecorderTask(),
                mImpressionsSyncHelper);
        mRetryTimer.start();
    }

    private void scheduleImpressionsRecorderTask() {
        if (mImpressionsRecorderTaskId != null) {
            mTaskExecutor.stopTask(mImpressionsRecorderTaskId);
        }
        mImpressionsRecorderTaskId = mTaskExecutor.schedule(
                mImpressionsTaskFactory.createImpressionsRecorderTask(),
                ServiceConstants.NO_INITIAL_DELAY,
                mImpressionsRefreshRate,
                mImpressionsSyncHelper);
    }
}
