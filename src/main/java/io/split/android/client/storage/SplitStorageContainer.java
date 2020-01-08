package io.split.android.client.storage;

import androidx.annotation.NonNull;

import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitStorageContainer {
    private final SplitsStorage mSplitStorage;
    private final MySegmentsStorage mMySegmentsStorage;

    public SplitStorageContainer(@NonNull SplitsStorage splitStorage,
                                 @NonNull MySegmentsStorage mySegmentsStorage) {
        mSplitStorage = checkNotNull(splitStorage);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
    }

    public SplitsStorage getSplitStorage() {
        return mSplitStorage;
    }

    public MySegmentsStorage getMySegmentsStorage() {
        return mMySegmentsStorage;
    }
}
