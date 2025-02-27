package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import io.split.android.client.utils.logger.Logger;

public class LazyRuleBasedSegmentStorageProvider implements RuleBasedSegmentStorageProvider {
    
    private final AtomicReference<RuleBasedSegmentStorage> mRuleBasedSegmentStorageRef = new AtomicReference<>();
    
    public void set(@NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage) {
        if (!mRuleBasedSegmentStorageRef.compareAndSet(null, ruleBasedSegmentStorage)) {
            Logger.w("RuleBasedSegmentStorage already set in LazyRuleBasedSegmentStorageProvider");
        }
    }
    
    @Nullable
    @Override
    public RuleBasedSegmentStorage get() {
        return mRuleBasedSegmentStorageRef.get();
    }
}
