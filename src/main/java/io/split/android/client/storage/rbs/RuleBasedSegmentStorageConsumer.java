package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import io.split.android.engine.experiments.ParsedRuleBasedSegment;

public interface RuleBasedSegmentStorageConsumer {

    @Nullable
    ParsedRuleBasedSegment get(String segmentName, String matchingKey);

    boolean contains(@NonNull Set<String> segmentNames);
}
