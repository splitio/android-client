package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.utils.logger.Logger;

public class LazyRuleBasedSegmentStorageProvider implements RuleBasedSegmentStorageProvider {
    
    private final AtomicReference<RuleBasedSegmentStorage> mRuleBasedSegmentStorageRef = new AtomicReference<>();
    
    public void set(@NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage) {
        if (!mRuleBasedSegmentStorageRef.compareAndSet(null, ruleBasedSegmentStorage)) {
            Logger.w("RuleBasedSegmentStorage already set in LazyRuleBasedSegmentStorageProvider");
        }
    }
    
    @NonNull
    @Override
    public RuleBasedSegmentStorage get() {
        RuleBasedSegmentStorage storage = mRuleBasedSegmentStorageRef.get();
        if (storage == null) {
            throw new IllegalStateException("RuleBasedSegmentStorage not set in LazyRuleBasedSegmentStorageProvider");
        }
        return storage;
    }
}
