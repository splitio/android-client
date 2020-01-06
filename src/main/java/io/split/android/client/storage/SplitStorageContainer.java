package io.split.android.client.storage;

import androidx.annotation.NonNull;

import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitStorageContainer {
    private final SplitsStorage mSplitStorage;

    public SplitStorageContainer(@NonNull SplitsStorage splitStorage) {
        mSplitStorage = checkNotNull(splitStorage);
    }

    public SplitsStorage getSplitStorage() {
        return mSplitStorage;
    }
}
