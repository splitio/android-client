package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitKillTask implements SplitTask {

    private final Split mKilledSplit;
    private final SplitsStorage mSplitsStorage;

    public SplitKillTask(@NonNull SplitsStorage splitsStorage, Split split) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mKilledSplit = split;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        try {
            if (mKilledSplit == null) {
                logError("Split name to kill could not be null.");
                return SplitTaskExecutionInfo.error(SplitTaskType.SPLIT_KILL);
            }
            long changeNumber = mSplitsStorage.getTill();

            if (mKilledSplit.changeNumber <= changeNumber) {
                Logger.d("Skipping killed split notification for old change number: "
                        + mKilledSplit.changeNumber);
                return SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL);
            }

            Split splitToKill = mSplitsStorage.get(mKilledSplit.name);
            splitToKill.killed = true;
            splitToKill.defaultTreatment = mKilledSplit.defaultTreatment;
            splitToKill.changeNumber = mKilledSplit.changeNumber;

            mSplitsStorage.updateWithoutChecks(splitToKill);
        } catch (Exception e) {
            logError("Unknown error while updating killed split: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLIT_KILL);
        }
        Logger.d("Killed split has been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL);
    }

    private void logError(String message) {
        Logger.e("Error while executing Split kill task: " + message);
    }
}
