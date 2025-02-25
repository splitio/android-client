package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.RolloutDefinitionsCache;

public interface RuleBasedSegmentStorage extends RolloutDefinitionsCache {

    @Nullable
    RuleBasedSegment get(String segmentName);

    boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber);

    long getChangeNumber();

    boolean contains(@NonNull Set<String> segmentNames);
}
