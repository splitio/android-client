package io.split.android.client.dtos;

import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

public class TargetingRulesChange {
    @SerializedName("ff")
    private SplitChange ff;

    @SerializedName("rbs")
    private RuleBasedSegmentChange rbs;

    public SplitChange getFeatureFlagsChange() {
        return ff;
    }

    public RuleBasedSegmentChange getRuleBasedSegmentsChange() {
        return rbs;
    }

    @VisibleForTesting
    public static TargetingRulesChange create(SplitChange splitChange) {
        return create(splitChange, RuleBasedSegmentChange.createEmpty());
    }

    @VisibleForTesting
    public static TargetingRulesChange create(SplitChange splitChange, RuleBasedSegmentChange ruleBasedSegmentChange) {
        TargetingRulesChange targetingRulesChange = new TargetingRulesChange();
        targetingRulesChange.ff = splitChange;
        targetingRulesChange.rbs = ruleBasedSegmentChange;
        return targetingRulesChange;
    }
}
