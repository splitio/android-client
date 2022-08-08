package io.split.android.client.storage.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MySegmentsStorageContainerImpl implements MySegmentsStorageContainer {

    private final ConcurrentMap<String, MySegmentsStorage> mStorageMap = new ConcurrentHashMap<>();
    private final PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    private final Object lock = new Object();

    public MySegmentsStorageContainerImpl(@NonNull PersistentMySegmentsStorage persistentMySegmentsStorage) {
        mPersistentMySegmentsStorage = checkNotNull(persistentMySegmentsStorage);
    }

    @NonNull
    @Override
    public MySegmentsStorage getStorageForKey(String matchingKey) {
        synchronized (lock) {
            if (mStorageMap.get(matchingKey) == null) {
                mStorageMap.put(matchingKey, new MySegmentsStorageImpl(matchingKey, mPersistentMySegmentsStorage));
            }

            return mStorageMap.get(matchingKey);
        }
    }

    @Override
    public long getUniqueAmount() {
        Set<String> segments = new HashSet<>();

        for (MySegmentsStorage mySegmentsStorage : mStorageMap.values()) {
            segments.addAll(mySegmentsStorage.getAll());
        }

        return segments.size();
    }
}
