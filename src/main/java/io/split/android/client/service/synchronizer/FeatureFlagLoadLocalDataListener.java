package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import io.split.android.client.TimeChecker;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;

class FeatureFlagLoadLocalDataListener extends LoadLocalDataListener {

    private volatile long mStartTime = -1;

    FeatureFlagLoadLocalDataListener(ISplitEventsManager splitEventsManager, SplitInternalEvent eventToFire) {
        super(splitEventsManager, eventToFire);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (mStartTime != -1) {
            TimeChecker.timeCheckerLog("Time for ready from cache process", mStartTime);
            TimeChecker.timeSinceStartLog("Time until feature flags process ended");
        }
        super.taskExecuted(taskInfo);
    }

    void setStartTime(long startTime) {
        mStartTime = startTime;
    }
}
