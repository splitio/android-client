package io.split.android.client.service.synchronizer.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;

public class MySegmentsSynchronizerFactoryImpl implements MySegmentsSynchronizerFactory {

    private final RetryBackoffCounterTimerFactory mRetryBackoffCounterTimerFactory;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final int mSegmentsRefreshRate;

    public MySegmentsSynchronizerFactoryImpl(@NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                                             @NonNull SplitTaskExecutor splitTaskExecutor,
                                             int segmentsRefreshRate) {
        mRetryBackoffCounterTimerFactory = checkNotNull(retryBackoffCounterTimerFactory);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mSegmentsRefreshRate = segmentsRefreshRate;
    }

    @Override
    public MySegmentsSynchronizer getSynchronizer(MySegmentsTaskFactory mySegmentsTaskFactory, SplitEventsManager splitEventsManager) {
        return new MySegmentsSynchronizerImpl(mRetryBackoffCounterTimerFactory.create(mSplitTaskExecutor, 1),
                mSplitTaskExecutor,
                splitEventsManager,
                mySegmentsTaskFactory,
                mSegmentsRefreshRate);
    }
}
