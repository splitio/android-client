package io.split.android.client.storage.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class MySegmentsStorageImpl implements MySegmentsStorage {

    private final String mMatchingKey;
    private final PersistentMySegmentsStorage mPersistentStorage;
    private final Set<String> mInMemoryMySegments;
    private final AtomicLong mTill;

    public MySegmentsStorageImpl(@NonNull String matchingKey, @NonNull PersistentMySegmentsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mMatchingKey = checkNotNull(matchingKey);
        mInMemoryMySegments = Collections.newSetFromMap(new ConcurrentHashMap<>());
        mTill = new AtomicLong(-1);
    }

    @Override
    public void loadLocal() {
        SegmentChangeDTO snapshot = mPersistentStorage.getSnapshot(mMatchingKey);
        mInMemoryMySegments.addAll(snapshot.getMySegments());
        mTill.set(snapshot.getTill());
    }

    @Override
    public Set<String> getAll() {
        return mInMemoryMySegments;
    }

    @Override
    public void set(@NonNull List<String> mySegments, long till) {
        if (mySegments == null) {
            return;
        }
        mInMemoryMySegments.clear();
        mInMemoryMySegments.addAll(mySegments);
        mTill.set(till);
        mPersistentStorage.set(mMatchingKey, mySegments, till);
    }

    @Override
    public long getTill() {
        return mTill.get();
    }

    @Override
    @VisibleForTesting
    public void clear() {
        mInMemoryMySegments.clear();
        mTill.set(-1);
        mPersistentStorage.set(mMatchingKey, new ArrayList<>(), -1);
    }
}
