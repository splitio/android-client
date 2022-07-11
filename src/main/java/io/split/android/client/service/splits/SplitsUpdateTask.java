package io.split.android.client.service.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.SplitsChangeChecker;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.Logger;

public class SplitsUpdateTask implements SplitTask {

    private final SplitsStorage mSplitsStorage;
    private final Long mChangeNumber;
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final ISplitEventsManager mEventsManager;
    private SplitsChangeChecker mChangeChecker;

    public SplitsUpdateTask(SplitsSyncHelper splitsSyncHelper,
                            SplitsStorage splitsStorage,
                            long since,
                            ISplitEventsManager eventsManager) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsSyncHelper = checkNotNull(splitsSyncHelper);
        mChangeNumber = since;
        mEventsManager = checkNotNull(eventsManager);
        mChangeChecker = new SplitsChangeChecker();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {

        if (mChangeNumber == null || mChangeNumber == 0) {
            Logger.e("Could not update split. Invalid change number " + mChangeNumber);
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }

        long storedChangeNumber = mSplitsStorage.getTill();
        if (mChangeNumber <= storedChangeNumber) {
            Logger.d("Received change number is previous than stored one. " +
                    "Avoiding update.");
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        }

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(mChangeNumber);
        if (result.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
            SplitInternalEvent event = SplitInternalEvent.SPLITS_FETCHED;
            if (mChangeChecker.splitsHaveChanged(storedChangeNumber, mSplitsStorage.getTill())) {
                event = SplitInternalEvent.SPLITS_UPDATED;
            }
            mEventsManager.notifyInternalEvent(event);
        }
        return result;
    }

    @VisibleForTesting
    public void setChangeChecker(SplitsChangeChecker changeChecker) {
        mChangeChecker = changeChecker;
    }
}
