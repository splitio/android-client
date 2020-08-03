package io.split.android.client.storage;

import androidx.annotation.NonNull;

import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitStorageContainer {

    private final SplitsStorage mSplitStorage;
    private final MySegmentsStorage mMySegmentsStorage;
    private final PersistentSplitsStorage mPersistentSplitsStorage;
    private final PersistentEventsStorage mPersistentEventsStorage;
    private final PersistentImpressionsStorage mPersistentImpressionsStorage;

    public SplitStorageContainer(@NonNull SplitsStorage splitStorage,
                                 @NonNull MySegmentsStorage mySegmentsStorage,
                                 @NonNull PersistentSplitsStorage persistentSplitsStorage,
                                 @NonNull PersistentEventsStorage persistentEventsStorage,
                                 @NonNull PersistentImpressionsStorage persistentImpressionsStorage) {

        mSplitStorage = checkNotNull(splitStorage);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mPersistentSplitsStorage = checkNotNull(persistentSplitsStorage);
        mPersistentEventsStorage = checkNotNull(persistentEventsStorage);
        mPersistentImpressionsStorage = checkNotNull(persistentImpressionsStorage);
    }

    public SplitsStorage getSplitsStorage() {
        return mSplitStorage;
    }

    public MySegmentsStorage getMySegmentsStorage() {
        return mMySegmentsStorage;
    }

    public PersistentSplitsStorage getPersistentSplitsStorage() {
        return mPersistentSplitsStorage;
    }

    public PersistentEventsStorage getEventsStorage() {
        return mPersistentEventsStorage;
    }

    public PersistentImpressionsStorage getImpressionsStorage() {
        return mPersistentImpressionsStorage;
    }

}