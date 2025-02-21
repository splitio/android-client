package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.RuleBasedSegment;

public class RuleBasedSegmentStorageImpl implements RuleBasedSegmentStorage {

    private final ConcurrentHashMap<String, RuleBasedSegment> mInMemorySegments;
    private final PersistentRuleBasedSegmentStorage mPersistentStorage;
    private volatile long mChangeNumber;

    public RuleBasedSegmentStorageImpl(@NonNull PersistentRuleBasedSegmentStorage persistentStorage) {
        mInMemorySegments = new ConcurrentHashMap<>();
        mPersistentStorage = checkNotNull(persistentStorage);
        mChangeNumber = -1;
    }

    @Nullable
    @Override
    public RuleBasedSegment get(String segmentName) {
        return mInMemorySegments.get(segmentName);
    }

    @Override
    public synchronized boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber) {
        boolean appliedUpdates = false;

        if (toAdd != null) {
            if (!toAdd.isEmpty()) {
                for (RuleBasedSegment segment : toAdd) {
                    mInMemorySegments.put(segment.getName(), segment);
                }

                appliedUpdates = true;
            }
        }

        if (toRemove != null) {
            if (!toRemove.isEmpty()) {
                for (RuleBasedSegment segment : toRemove) {
                    mInMemorySegments.remove(segment.getName());
                }
            }
        }

        mChangeNumber = changeNumber;

        return appliedUpdates;
    }

    @Override
    public long getChangeNumber() {
        return mChangeNumber;
    }

    @Override
    public boolean contains(@NonNull Set<String> segmentNames) {
        if (segmentNames == null) {
            return false;
        }

        for (String name : segmentNames) {
            if (!mInMemorySegments.containsKey(name)) {
                return false;
            }
        }
        return true;
    }

    @WorkerThread
    @Override
    public void loadLocal() {
        RuleBasedSegmentSnapshot snapshot = mPersistentStorage.getSnapshot();
        Set<RuleBasedSegment> segments = snapshot.getSegments();
        mChangeNumber = snapshot.getChangeNumber();
        for (RuleBasedSegment segment : segments) {
            mInMemorySegments.put(segment.getName(), segment);
        }
    }

    @WorkerThread
    @Override
    public void clear() {
        mInMemorySegments.clear();
        mChangeNumber = -1;
        mPersistentStorage.clear();
    }
}
