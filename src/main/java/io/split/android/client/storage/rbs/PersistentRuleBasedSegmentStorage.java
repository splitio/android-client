package io.split.android.client.storage.rbs;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;

public interface PersistentRuleBasedSegmentStorage {

    RuleBasedSegmentSnapshot getSnapshot();

    boolean update(Set<RuleBasedSegment> toAdd, Set<RuleBasedSegment> toRemove, long changeNumber);

    void clear();
}
