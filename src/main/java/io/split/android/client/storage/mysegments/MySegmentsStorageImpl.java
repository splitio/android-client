package io.split.android.client.storage.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class MySegmentsStorageImpl implements MySegmentsStorage {

    private final String mMatchingKey;
    private final PersistentMySegmentsStorage mPersistentStorage;
    private final Set<String> mInMemoryMySegments;

    public MySegmentsStorageImpl(@NonNull String matchingKey, @NonNull PersistentMySegmentsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mMatchingKey = checkNotNull(matchingKey);
        mInMemoryMySegments = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
