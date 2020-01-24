package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySegmentsStorageImpl implements MySegmentsStorage {

    private PersistentMySegmentsStorage mPersistentStorage;
    private Set<String> mInMemoryMySegments;


    public MySegmentsStorageImpl(@NonNull PersistentMySegmentsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mInMemoryMySegments = Sets.newConcurrentHashSet();
    }

    @Override
    public Set<String> getAll() {
        return mInMemoryMySegments;
    }

    @Override
    public void set(List<String> mySegments) {
        if (mySegments == null) {
            return;
        }
        mInMemoryMySegments.clear();
        mInMemoryMySegments.addAll(mySegments);
        mPersistentStorage.set(mySegments);
    }

    @Override
    public void clear() {
        mInMemoryMySegments.clear();
        mPersistentStorage.set(new ArrayList<>());
    }

    @Override
    public void loadFromDisk() {
        mInMemoryMySegments.addAll(mPersistentStorage.getSnapshot());
    }
}
