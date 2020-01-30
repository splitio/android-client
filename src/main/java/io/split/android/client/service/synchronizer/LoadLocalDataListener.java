package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;

import static com.google.common.base.Preconditions.checkNotNull;

class LoadLocalDataListener implements SplitTaskExecutionListener {

    private final SplitEventsManager mSplitEventsManager;
    private final SplitInternalEvent mEventToFire;

    public LoadLocalDataListener(SplitEventsManager splitEventsManager,
                                 SplitInternalEvent eventToFire) {
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mEventToFire = checkNotNull(eventToFire);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (taskInfo.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
            mSplitEventsManager.notifyInternalEvent(mEventToFire);
        }
    }
}
