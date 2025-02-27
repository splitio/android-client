package io.split.android.client.localhost;

import androidx.annotation.NonNull;

import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProvider;

class LocalhostRuleBasedSegmentsStorageProvider implements RuleBasedSegmentStorageProvider {
    
    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    
    LocalhostRuleBasedSegmentsStorageProvider(@NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage) {
        mRuleBasedSegmentStorage = ruleBasedSegmentStorage;
    }
    
    @NonNull
    @Override
    public RuleBasedSegmentStorage get() {
        return mRuleBasedSegmentStorage;
    }
}
