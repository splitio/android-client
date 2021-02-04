package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class CleanUpDatabaseTask implements SplitTask {

    private final PersistentEventsStorage mEventsStorage;
    private final PersistentImpressionsStorage mImpressionsStorage;
    private final long mMaxTimestamp;

    public CleanUpDatabaseTask(PersistentEventsStorage eventsStorage,
                               PersistentImpressionsStorage impressionsStorage,
                               long maxTimestamp) {
        mEventsStorage = checkNotNull(eventsStorage);
        mImpressionsStorage = checkNotNull(impressionsStorage);
        mMaxTimestamp = maxTimestamp;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mEventsStorage.deleteInvalid(mMaxTimestamp);
        mImpressionsStorage.deleteInvalid(mMaxTimestamp);
        return SplitTaskExecutionInfo.error(SplitTaskType.CLEAN_UP_DATABASE);
    }
}
