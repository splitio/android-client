package io.split.android.client.service.synchronizer.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
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
    private String mMySegmentsFetcherTaskId;

    public MySegmentsSynchronizerImpl(@NonNull RetryBackoffCounterTimer retryBackoffCounterTimer,
                                      @NonNull SplitTaskExecutor taskExecutor,
                                      @NonNull SplitEventsManager eventsManager,
                                      @NonNull MySegmentsTaskFactory mySegmentsTaskFactory,
                                      int segmentsRefreshRate) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mMySegmentsSyncRetryTimer = checkNotNull(retryBackoffCounterTimer);
        mSplitTaskFactory = checkNotNull(mySegmentsTaskFactory);
        mSegmentsRefreshRate = segmentsRefreshRate;
        mLoadLocalMySegmentsListener = new LoadLocalDataListener(
                eventsManager, SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
    }

    @Override
    public void loadMySegmentsFromCache() {
        submitMySegmentsLoadingTask(mLoadLocalMySegmentsListener);
    }

    @Override
    public void synchronizeMySegments() {
        mMySegmentsSyncRetryTimer.setTask(mSplitTaskFactory.createMySegmentsSyncTask(false), null);
        mMySegmentsSyncRetryTimer.start();
    }

    @Override
    public void forceMySegmentsSync() {
        mMySegmentsSyncRetryTimer.setTask(mSplitTaskFactory.createMySegmentsSyncTask(true), null);
        mMySegmentsSyncRetryTimer.start();
    }

    @Override
    public void destroy() {
        mMySegmentsSyncRetryTimer.stop();
    }

    @Override
    public void scheduleSegmentsSyncTask() {
        mMySegmentsFetcherTaskId = mTaskExecutor.schedule(
                mSplitTaskFactory.createMySegmentsSyncTask(false),
                mSegmentsRefreshRate,
                mSegmentsRefreshRate,
                null);
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
