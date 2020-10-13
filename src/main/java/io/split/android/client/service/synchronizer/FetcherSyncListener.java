package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;

import static com.google.common.base.Preconditions.checkNotNull;

class FetcherSyncListener implements SplitTaskExecutionListener {

    private AtomicBoolean mIsFirstFetch;
    private final SplitEventsManager mSplitEventsManager;
    private final SplitInternalEvent mEventToFireOnFirstTime;

    public FetcherSyncListener(SplitEventsManager splitEventsManager,
                               SplitInternalEvent eventToFireOnFirstTime) {
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mEventToFireOnFirstTime = checkNotNull(eventToFireOnFirstTime);
        mIsFirstFetch = new AtomicBoolean(true);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (mIsFirstFetch.get() && taskInfo.getStatus().equals(SplitTaskExecutionStatus.SUCCESS)) {
            mIsFirstFetch.set(false);
            mSplitEventsManager.notifyInternalEvent(mEventToFireOnFirstTime);
        }
    }
}
