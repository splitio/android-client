package io.split.android.client.service.rules;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;

public class ProcessedRuleBasedSegmentChange {
    private final Set<RuleBasedSegment> mActive;
    private final Set<RuleBasedSegment> mArchived;
    private final long mChangeNumber;
    private final long mUpdateTimestamp;

    public ProcessedRuleBasedSegmentChange(Set<RuleBasedSegment> active,
                                           Set<RuleBasedSegment> archived,
                                           long changeNumber,
                                           long updateTimestamp) {
        mActive = active;
        mArchived = archived;
        mChangeNumber = changeNumber;
        mUpdateTimestamp = updateTimestamp;
    }

    public Set<RuleBasedSegment> getActive() {
        return mActive;
    }

    public Set<RuleBasedSegment> getArchived() {
        return mArchived;
    }

    public long getChangeNumber() {
        return mChangeNumber;
    }

    public long getUpdateTimestamp() {
        return mUpdateTimestamp;
    }
}