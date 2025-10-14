package io.split.android.client.localhost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.engine.experiments.ParsedRuleBasedSegment;

public class LocalhostRuleBasedSegmentsStorage implements RuleBasedSegmentStorage {

    @Nullable
    @Override
    public ParsedRuleBasedSegment get(String segmentName, String matchingKey) {
        return null;
    }

    @Override
    public boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber, ExecutorService executor) {
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
