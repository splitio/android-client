package io.split.android.client.storage;

import androidx.annotation.NonNull;

import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitStorageContainer {
    private final SplitsStorage mSplitStorage;
    private final MySegmentsStorage mMySegmentsStorage;
    private final PersistentEventsStorage mPersistentEventsStorage;

    public SplitStorageContainer(@NonNull SplitsStorage splitStorage,
                                 @NonNull MySegmentsStorage mySegmentsStorage,
                                 @NonNull PersistentEventsStorage persistentEventsStorage) {

        mSplitStorage = checkNotNull(splitStorage);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mPersistentEventsStorage = checkNotNull(persistentEventsStorage);
    }

    public SplitsStorage getSplitsStorage() {
        return mSplitStorage;
    }

    public MySegmentsStorage getMySegmentsStorage() {
        return mMySegmentsStorage;
    }

    public PersistentEventsStorage getEventsStorage() {
        return mPersistentEventsStorage;
    }
}
