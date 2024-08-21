package io.split.android.client.service.synchronizer.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.LoadLocalDataListener;

public class MySegmentsSynchronizerImpl implements MySegmentsSynchronizer {

    private final RetryBackoffCounterTimer mMySegmentsSyncRetryTimer;
    private final SplitTaskExecutor mTaskExecutor;
    private final MySegmentsTaskFactory mSplitTaskFactory;
    private final int mSegmentsRefreshRate;
    private final LoadLocalDataListener mLoadLocalMySegmentsListener;
    private final SplitTaskExecutionListener mMySegmentsSyncListener;
    private final AtomicBoolean mIsSynchronizing = new AtomicBoolean(true);
    private String mMySegmentsFetcherTaskId;

    public MySegmentsSynchronizerImpl(@NonNull RetryBackoffCounterTimer retryBackoffCounterTimer,
                                      @NonNull SplitTaskExecutor taskExecutor,
                                      @NonNull SplitEventsManager eventsManager,
                                      @NonNull MySegmentsTaskFactory mySegmentsTaskFactory,
                                      int segmentsRefreshRate,
                                      @NonNull SplitInternalEvent loadedFromStorageInternalEvent) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mMySegmentsSyncRetryTimer = checkNotNull(retryBackoffCounterTimer);
        mSplitTaskFactory = checkNotNull(mySegmentsTaskFactory);
        mSegmentsRefreshRate = segmentsRefreshRate;
        mLoadLocalMySegmentsListener = new LoadLocalDataListener(
                eventsManager, loadedFromStorageInternalEvent);
        mMySegmentsSyncListener = new SplitTaskExecutionListener() {
            @Override
            public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                if (taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR) {
                    if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                        mIsSynchronizing.compareAndSet(true, false);
                        stopPeriodicFetching();
                    }
                }
            }
        };
    }

    @Override
    public void loadMySegmentsFromCache() {
        submitMySegmentsLoadingTask(mLoadLocalMySegmentsListener);
    }

    @Override
    public void synchronizeMySegments() {
        if (mIsSynchronizing.get()) {
            mMySegmentsSyncRetryTimer.setTask(mSplitTaskFactory.createMySegmentsSyncTask(false), mMySegmentsSyncListener);
            mMySegmentsSyncRetryTimer.start();
        }
    }

    @Override
    public void forceMySegmentsSync(Long syncDelay) {
        if (mIsSynchronizing.get()) {
            mMySegmentsSyncRetryTimer.stop();
            mMySegmentsSyncRetryTimer.setTask(mSplitTaskFactory.createMySegmentsSyncTask(true), syncDelay, mMySegmentsSyncListener);
            mMySegmentsSyncRetryTimer.start();
        }
    }

    @Override
    public void destroy() {
        mMySegmentsSyncRetryTimer.stop();
    }

    @Override
    public void scheduleSegmentsSyncTask() {
        if (mIsSynchronizing.get()) {
            if (mMySegmentsFetcherTaskId != null) {
                // if a fetch task is already scheduled, we don't need to schedule another one
                return;
            }

            mMySegmentsFetcherTaskId = mTaskExecutor.schedule(
                    mSplitTaskFactory.createMySegmentsSyncTask(false),
                    mSegmentsRefreshRate,
                    mSegmentsRefreshRate,
                    mMySegmentsSyncListener);
        }
    }

    @Override
    public void stopPeriodicFetching() {
        mTaskExecutor.stopTask(mMySegmentsFetcherTaskId);
    }

    @Override
    public void submitMySegmentsLoadingTask() {
        submitMySegmentsLoadingTask(null);
    }

    private void submitMySegmentsLoadingTask(SplitTaskExecutionListener executionListener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadMySegmentsTask(), executionListener);
    }
}
