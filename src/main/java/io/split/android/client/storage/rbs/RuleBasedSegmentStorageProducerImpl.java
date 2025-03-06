package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.dtos.RuleBasedSegment;

public class RuleBasedSegmentStorageProducerImpl implements RuleBasedSegmentStorageProducer {

    private final ConcurrentHashMap<String, RuleBasedSegment> mInMemorySegments;
    private final PersistentRuleBasedSegmentStorage mPersistentStorage;
    private final AtomicLong mChangeNumberRef;

    public RuleBasedSegmentStorageProducerImpl(@NonNull PersistentRuleBasedSegmentStorage persistentStorage,
                                               @NonNull ConcurrentHashMap<String, RuleBasedSegment> segments,
                                               @NonNull AtomicLong changeNumberRef) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mInMemorySegments = checkNotNull(segments);
        mChangeNumberRef = checkNotNull(changeNumberRef);
    }

    @Override
    public boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber) {
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

        mChangeNumberRef.set(changeNumber);

        return appliedUpdates;
    }

    @Override
    public void loadLocal() {
        RuleBasedSegmentSnapshot snapshot = mPersistentStorage.getSnapshot();
        Map<String, RuleBasedSegment> segments = snapshot.getSegments();
        mChangeNumberRef.set(snapshot.getChangeNumber());
        mInMemorySegments.putAll(segments);
    }

    @Override
    public void clear() {
        mInMemorySegments.clear();
        mChangeNumberRef.set(-1);
        mPersistentStorage.clear();
    }
}
