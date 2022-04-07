package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;

public class LoadLocalDataListener implements SplitTaskExecutionListener {

    private final ISplitEventsManager mSplitEventsManager;
    private final SplitInternalEvent mEventToFire;

    public LoadLocalDataListener(ISplitEventsManager splitEventsManager,
                                 SplitInternalEvent eventToFire) {
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mEventToFire = checkNotNull(eventToFire);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (taskInfo.getStatus().equals(SplitTaskExecutionStatus.SUCCESS)) {
            mSplitEventsManager.notifyInternalEvent(mEventToFire);
        }
    }
}
