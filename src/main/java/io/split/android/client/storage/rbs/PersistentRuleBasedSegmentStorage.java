package io.split.android.client.storage.rbs;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;

public interface PersistentRuleBasedSegmentStorage {

    RuleBasedSegmentSnapshot getSnapshot();

    void update(Set<RuleBasedSegment> toAdd, Set<RuleBasedSegment> toRemove, long changeNumber);

    void clear();
}
