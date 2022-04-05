package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

class MySegmentsStorageImpl implements MySegmentsStorage {

    private final String mMatchingKey;
    private final PersistentMySegmentsStorage mPersistentStorage;
    private final Set<String> mInMemoryMySegments;

    public MySegmentsStorageImpl(@NonNull String matchingKey, @NonNull PersistentMySegmentsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mMatchingKey = checkNotNull(matchingKey);
        mInMemoryMySegments = Sets.newConcurrentHashSet();
    }

    @Override
    public void loadLocal() {
        mInMemoryMySegments.addAll(mPersistentStorage.getSnapshot(mMatchingKey));
    }

    @Override
    public Set<String> getAll() {
        return mInMemoryMySegments;
    }

    @Override
    public void set(@NonNull List<String> mySegments) {
        if (mySegments == null) {
            return;
        }
        mInMemoryMySegments.clear();
        mInMemoryMySegments.addAll(mySegments);
        mPersistentStorage.set(mMatchingKey, mySegments);
    }

    @Override
    public void clear() {
        mInMemoryMySegments.clear();
        mPersistentStorage.set(mMatchingKey, new ArrayList<>());
    }
}
