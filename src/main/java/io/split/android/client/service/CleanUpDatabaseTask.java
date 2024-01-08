package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;

import static io.split.android.client.utils.Utils.checkNotNull;

public class CleanUpDatabaseTask implements SplitTask {

    private final PersistentEventsStorage mEventsStorage;
    private final PersistentImpressionsStorage mImpressionsStorage;
    private final PersistentImpressionsCountStorage mImpressionsCountStorage;
    private final PersistentImpressionsUniqueStorage mImpressionsUniqueStorage;
    private final long mMaxTimestamp;

    public CleanUpDatabaseTask(PersistentEventsStorage eventsStorage,
                               PersistentImpressionsStorage impressionsStorage,
                               PersistentImpressionsCountStorage persistentImpressionsCountStorage,
                               PersistentImpressionsUniqueStorage persistentImpressionsUniqueStorage,
                               long maxTimestamp) {
        mEventsStorage = checkNotNull(eventsStorage);
        mImpressionsStorage = checkNotNull(impressionsStorage);
        mImpressionsCountStorage = checkNotNull(persistentImpressionsCountStorage);
        mImpressionsUniqueStorage = checkNotNull(persistentImpressionsUniqueStorage);
        mMaxTimestamp = maxTimestamp;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mEventsStorage.deleteInvalid(mMaxTimestamp);
        mImpressionsStorage.deleteInvalid(mMaxTimestamp);
        mImpressionsCountStorage.deleteInvalid(mMaxTimestamp);
        mImpressionsUniqueStorage.deleteInvalid(mMaxTimestamp);
        return SplitTaskExecutionInfo.error(SplitTaskType.CLEAN_UP_DATABASE);
    }
}
