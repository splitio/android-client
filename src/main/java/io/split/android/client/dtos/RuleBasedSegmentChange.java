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
        RuleBasedSegmentChange ruleBasedSegmentChange = new RuleBasedSegmentChange();
        ruleBasedSegmentChange.segments = new ArrayList<>();
        ruleBasedSegmentChange.since = -1;
        ruleBasedSegmentChange.till = -1;
        return ruleBasedSegmentChange;
    }
}
