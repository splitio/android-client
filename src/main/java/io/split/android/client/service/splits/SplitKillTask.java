package io.split.android.client.service.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.Split;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitKillTask implements SplitTask {

    private final Split mKilledSplit;
    private final SplitsStorage mSplitsStorage;
    private final ISplitEventsManager mEventsManager;

    public SplitKillTask(@NonNull SplitsStorage splitsStorage, Split split,
                         ISplitEventsManager eventsManager) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mKilledSplit = split;
        mEventsManager = eventsManager;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        try {
            if (mKilledSplit == null) {
                logError("Feature flag name to kill could not be null.");
                return SplitTaskExecutionInfo.error(SplitTaskType.SPLIT_KILL);
            }
            long changeNumber = mSplitsStorage.getTill();

            if (mKilledSplit.changeNumber <= changeNumber) {
                Logger.d("Skipping killed feature flag notification for old change number: "
                        + mKilledSplit.changeNumber);
                return SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL);
            }

            Split splitToKill = mSplitsStorage.get(mKilledSplit.name);
            if (splitToKill == null) {
                Logger.d("Skipping " + mKilledSplit.name + " since not in storage");
                return SplitTaskExecutionInfo.error(SplitTaskType.SPLIT_KILL);
            }

            splitToKill.killed = true;
            splitToKill.defaultTreatment = mKilledSplit.defaultTreatment;
            splitToKill.changeNumber = mKilledSplit.changeNumber;

            mSplitsStorage.updateWithoutChecks(splitToKill);
            mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION);
        } catch (Exception e) {
            logError("Unknown error while updating killed feature flag: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLIT_KILL);
        }
        Logger.d("Killed feature flag has been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL);
    }

    private void logError(String message) {
        Logger.e("Error while executing feature flag kill task: " + message);
    }
}
