package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.observer.PersistentImpressionsObserverCacheStorage;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsUniqueStorage;

import static io.split.android.client.utils.Utils.checkNotNull;

import java.util.concurrent.TimeUnit;

public class CleanUpDatabaseTask implements SplitTask {

    private final PersistentEventsStorage mEventsStorage;
    private final PersistentImpressionsStorage mImpressionsStorage;
    private final PersistentImpressionsCountStorage mImpressionsCountStorage;
    private final PersistentImpressionsUniqueStorage mImpressionsUniqueStorage;
    private final PersistentImpressionsObserverCacheStorage mImpressionsObserverCacheStorage;
    private final long mMaxTimestamp;

    public CleanUpDatabaseTask(PersistentEventsStorage eventsStorage,
                               PersistentImpressionsStorage impressionsStorage,
                               PersistentImpressionsCountStorage persistentImpressionsCountStorage,
                               PersistentImpressionsUniqueStorage persistentImpressionsUniqueStorage,
                               PersistentImpressionsObserverCacheStorage persistentImpressionsObserverCacheStorage,
                               long maxTimestamp) {
        mEventsStorage = checkNotNull(eventsStorage);
        mImpressionsStorage = checkNotNull(impressionsStorage);
        mImpressionsCountStorage = checkNotNull(persistentImpressionsCountStorage);
        mImpressionsUniqueStorage = checkNotNull(persistentImpressionsUniqueStorage);
        mImpressionsObserverCacheStorage = checkNotNull(persistentImpressionsObserverCacheStorage);
        mMaxTimestamp = maxTimestamp;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        try {
            mEventsStorage.deleteInvalid(mMaxTimestamp);
            mImpressionsStorage.deleteInvalid(mMaxTimestamp);
            mImpressionsCountStorage.deleteInvalid(mMaxTimestamp);
            mImpressionsUniqueStorage.deleteInvalid(mMaxTimestamp);
            mImpressionsObserverCacheStorage.deleteOutdated(TimeUnit.SECONDS.toMillis(mMaxTimestamp));
            return SplitTaskExecutionInfo.success(SplitTaskType.CLEAN_UP_DATABASE);
        } catch (Throwable t) {
            return SplitTaskExecutionInfo.error(SplitTaskType.CLEAN_UP_DATABASE);
        }
    }
}
