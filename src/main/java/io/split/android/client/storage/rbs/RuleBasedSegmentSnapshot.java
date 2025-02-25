package io.split.android.client.storage.rbs;

import static io.split.android.client.utils.Utils.checkNotNull;

import java.util.Map;

import io.split.android.client.dtos.RuleBasedSegment;

public class RuleBasedSegmentSnapshot {

    private final Map<String, RuleBasedSegment> mSegments;

    private final long mChangeNumber;

    public RuleBasedSegmentSnapshot(Map<String, RuleBasedSegment> segments, long changeNumber) {
        mSegments = checkNotNull(segments);
        mChangeNumber = changeNumber;
    }

    public Map<String, RuleBasedSegment> getSegments() {
        return mSegments;
    }

    public long getChangeNumber() {
        return mChangeNumber;
    }
}
