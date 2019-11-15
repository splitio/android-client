package io.split.android.client.storage;

import androidx.annotation.NonNull;

import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitStorageProvider {
    private final SplitsStorage mSplitStorage;

    public SplitStorageProvider(@NonNull SplitsStorage splitStorage) {
        checkNotNull(splitStorage);

        mSplitStorage = splitStorage;
    }

    public SplitsStorage getSplitStorage() {
        return mSplitStorage;
    }
}
