package io.split.android.client.service.rules;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.Status;

public class RuleBasedSegmentChangeProcessor {

    public ProcessedRuleBasedSegmentChange process(List<RuleBasedSegment> segments, long changeNumber) {
        Set<RuleBasedSegment> toAdd = new HashSet<>();
        Set<RuleBasedSegment> toRemove = new HashSet<>();
        for (RuleBasedSegment segment : segments) {
            if (segment.getStatus() == Status.ACTIVE) {
                toAdd.add(segment);
            } else {
                toRemove.add(segment);
            }
        }

        return new ProcessedRuleBasedSegmentChange(toAdd, toRemove, changeNumber, System.currentTimeMillis());
    }

    public ProcessedRuleBasedSegmentChange process(RuleBasedSegment segment, long changeNumber) {
        return process(Collections.singletonList(segment), changeNumber);
    }
}
