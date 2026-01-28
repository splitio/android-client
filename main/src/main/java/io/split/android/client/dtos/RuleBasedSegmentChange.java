package io.split.android.client.dtos;

import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedSegmentChange {

    @SerializedName("s")
    private long since;

    @SerializedName("t")
    private long till;

    @SerializedName("d")
    private List<RuleBasedSegment> segments;

    public long getSince() {
        return since;
    }

    public long getTill() {
        return till;
    }

    public List<RuleBasedSegment> getSegments() {
        return segments;
    }

    @VisibleForTesting
    public static RuleBasedSegmentChange createEmpty() {
        return create(-1, -1, new ArrayList<>());
    }

    @VisibleForTesting
    public static RuleBasedSegmentChange create(long since, long till, List<RuleBasedSegment> segments) {
        RuleBasedSegmentChange ruleBasedSegmentChange = new RuleBasedSegmentChange();
        ruleBasedSegmentChange.segments = segments;
        ruleBasedSegmentChange.since = since;
        ruleBasedSegmentChange.till = till;
        return ruleBasedSegmentChange;
    }
}
