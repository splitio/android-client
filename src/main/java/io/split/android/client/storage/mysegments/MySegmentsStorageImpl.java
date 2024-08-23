package io.split.android.client.storage.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.dtos.Segment;
import io.split.android.client.dtos.SegmentsChange;

class MySegmentsStorageImpl implements MySegmentsStorage {

    public static final int DEFAULT_CHANGE_NUMBER = -1;
    private final String mMatchingKey;
    private final PersistentMySegmentsStorage mPersistentStorage;
    private final Set<String> mInMemoryMySegments;
    private final AtomicLong mTill;

    public MySegmentsStorageImpl(@NonNull String matchingKey, @NonNull PersistentMySegmentsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mMatchingKey = checkNotNull(matchingKey);
        mInMemoryMySegments = Collections.newSetFromMap(new ConcurrentHashMap<>());
        mTill = new AtomicLong(DEFAULT_CHANGE_NUMBER);
    }

    @Override
    public void loadLocal() {
        SegmentsChange snapshot = mPersistentStorage.getSnapshot(mMatchingKey);
        mInMemoryMySegments.addAll(toNames(snapshot.getSegments()));
        mTill.set(getOrDefault(snapshot.getChangeNumber()));
    }

    @Override
    public Set<String> getAll() {
        return mInMemoryMySegments;
    }

    @Override
    public void set(@NonNull SegmentsChange segmentsChange) {
        if (segmentsChange == null) {
            return;
        }
        mInMemoryMySegments.clear();
        mInMemoryMySegments.addAll(toNames(segmentsChange.getSegments()));
        mTill.set(getOrDefault(segmentsChange.getChangeNumber()));
        mPersistentStorage.set(mMatchingKey, segmentsChange);
    }

    @Override
    public long getTill() {
        return mTill.get();
    }

    @Override
    @VisibleForTesting
    public void clear() {
        mInMemoryMySegments.clear();
        mTill.set(DEFAULT_CHANGE_NUMBER);
        mPersistentStorage.set(mMatchingKey, SegmentsChange.createEmpty());
    }

    @NonNull
    private static Set<String> toNames(Set<Segment> segments) {
        if (segments == null) {
            return Collections.emptySet();
        }

        Set<String> names = new HashSet<>();
        for (Segment segment : segments) {
            names.add(segment.getName());
        }

        return names;
    }

    @NonNull
    private static Long getOrDefault(@Nullable Long changeNumber) {
        return changeNumber == null ? DEFAULT_CHANGE_NUMBER : changeNumber;
    }
}
