package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;

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
        System.out.println(StartupTimeTracker.getElapsedTimeLog("Task executed with status: " + taskInfo.getStatus() + " for event: " + mEventToFire.name()));
        if (taskInfo.getStatus().equals(SplitTaskExecutionStatus.SUCCESS)) {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("Notifying internal event: " + mEventToFire.name()));
            mSplitEventsManager.notifyInternalEvent(mEventToFire);
        } else {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("Not notifying internal event due to task failure: " + mEventToFire.name()));
        }
    }
}
