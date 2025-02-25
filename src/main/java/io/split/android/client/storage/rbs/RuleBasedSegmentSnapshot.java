package io.split.android.client.storage.rbs;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;

public class RuleBasedSegmentSnapshot {

    private final Set<RuleBasedSegment> mSegments;

    private final long mChangeNumber;

    public RuleBasedSegmentSnapshot(Set<RuleBasedSegment> segments, long changeNumber) {
        mSegments = segments;
        mChangeNumber = changeNumber;
    }

    public Set<RuleBasedSegment> getSegments() {
        return mSegments;
    }

    public long getChangeNumber() {
        return mChangeNumber;
    }
}
