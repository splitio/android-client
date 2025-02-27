package io.split.android.client.localhost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;

public class LocalhostRuleBasedSegmentsStorage implements RuleBasedSegmentStorage {

    @Nullable
    @Override
    public RuleBasedSegment get(String segmentName) {
        return null;
    }

    @Override
    public boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber) {
        return false;
    }

    @Override
    public long getChangeNumber() {
        return -1;
    }

    @Override
    public boolean contains(@NonNull Set<String> segmentNames) {
        return false;
    }

    @Override
    public void loadLocal() {
        // no-op
    }

    @Override
    public void clear() {
        // no-op
    }
}
