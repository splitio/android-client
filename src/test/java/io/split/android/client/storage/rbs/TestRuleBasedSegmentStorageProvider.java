package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;

/**
 * A simple provider implementation for tests.
 */
public class TestRuleBasedSegmentStorageProvider implements RuleBasedSegmentStorageProvider {
    
    private final RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    
    public TestRuleBasedSegmentStorageProvider(@NonNull RuleBasedSegmentStorage ruleBasedSegmentStorage) {
        mRuleBasedSegmentStorage = ruleBasedSegmentStorage;
    }
    
    @NonNull
    @Override
    public RuleBasedSegmentStorage get() {
        return mRuleBasedSegmentStorage;
    }
}
