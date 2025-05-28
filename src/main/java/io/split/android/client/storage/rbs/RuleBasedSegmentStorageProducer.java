package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;

import java.util.Set;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.storage.RolloutDefinitionsCache;

public interface RuleBasedSegmentStorageProducer extends RolloutDefinitionsCache {

    boolean update(@NonNull Set<RuleBasedSegment> toAdd, @NonNull Set<RuleBasedSegment> toRemove, long changeNumber);

    long getChangeNumber();
}
