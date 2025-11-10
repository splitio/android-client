package io.split.android.client.service.synchronizer.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;

public class MySegmentsSynchronizerFactoryImpl implements MySegmentsSynchronizerFactory {

    private static final int BACKOFF_BASE = 1;

    private final RetryBackoffCounterTimerFactory mRetryBackoffCounterTimerFactory;
    private final SplitTaskExecutor mSplitTaskExecutor;

    public MySegmentsSynchronizerFactoryImpl(@NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                                             @NonNull SplitTaskExecutor splitTaskExecutor) {
        mRetryBackoffCounterTimerFactory = checkNotNull(retryBackoffCounterTimerFactory);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
    }

    @Override
    public MySegmentsSynchronizer getSynchronizer(MySegmentsTaskFactory mySegmentsTaskFactory, SplitEventsManager splitEventsManager, SplitInternalEvent loadedFromStorageInternalEvent, int segmentsRefreshRate) {
        return new MySegmentsSynchronizerImpl(mRetryBackoffCounterTimerFactory.create(mSplitTaskExecutor, BACKOFF_BASE),
                mSplitTaskExecutor,
                splitEventsManager,
                mySegmentsTaskFactory,
                segmentsRefreshRate,
                loadedFromStorageInternalEvent);
    }
}
