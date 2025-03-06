package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.engine.experiments.ParsedRuleBasedSegment;
import io.split.android.engine.experiments.RuleBasedSegmentParser;

public class RuleBasedSegmentStorageImpl implements RuleBasedSegmentStorage {

    private final ConcurrentHashMap<String, RuleBasedSegment> mInMemorySegments;
    @Nullable
    private final RuleBasedSegmentParser mParser;
    private final RuleBasedSegmentStorageProducer mProducer;
    private final AtomicLong mChangeNumberRef;

    public RuleBasedSegmentStorageImpl(@NonNull PersistentRuleBasedSegmentStorage persistentStorage, @NonNull RuleBasedSegmentParser parser) {
        mInMemorySegments = new ConcurrentHashMap<>();
        mParser = checkNotNull(parser);
        mChangeNumberRef = new AtomicLong(-1);
        mProducer = new RuleBasedSegmentStorageProducerImpl(persistentStorage, mInMemorySegments, mChangeNumberRef);
    }

    @VisibleForTesting
    RuleBasedSegmentStorageImpl(RuleBasedSegmentStorageProducer producer,
                                @NonNull RuleBasedSegmentParser parser,
                                @NonNull ConcurrentHashMap<String, RuleBasedSegment> inMemorySegmentsMap,
                                @NonNull AtomicLong changeNumberRef) {
        mInMemorySegments = checkNotNull(inMemorySegmentsMap);
        mParser = checkNotNull(parser);
        mChangeNumberRef = checkNotNull(changeNumberRef);
        mProducer = checkNotNull(producer);
    }

    @Override
    public @Nullable ParsedRuleBasedSegment get(String segmentName, String matchingKey) {
        RuleBasedSegment ruleBasedSegment = mInMemorySegments.get(segmentName);
        if (ruleBasedSegment == null) {
            return null;
        }

        return mParser.parse(ruleBasedSegment, matchingKey);
    }

    @Override
    public synchronized boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber) {
        return mProducer.update(toAdd, toRemove, changeNumber);
    }

    @Override
    public long getChangeNumber() {
        return mChangeNumberRef.get();
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
    public synchronized void loadLocal() {
        mProducer.loadLocal();
    }

    @WorkerThread
    @Override
    public void clear() {
        mProducer.clear();
    }
}
