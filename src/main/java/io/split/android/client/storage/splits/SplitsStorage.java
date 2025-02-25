package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.RolloutDefinitionsCache;

public interface SplitsStorage extends RolloutDefinitionsCache {

    Split get(@NonNull String name);

    Map<String, Split> getMany(@Nullable List<String> splits);

    Map<String, Split> getAll();

    // Returns true if at least one split was updated
    boolean update(ProcessedSplitChange splitChange);

    void updateWithoutChecks(Split split);

    boolean isValidTrafficType(@Nullable String name);

    long getTill();

    long getUpdateTimestamp();

    String getSplitsFilterQueryString();

    void updateSplitsFilterQueryString(String queryString);

    String getFlagsSpec();

    void updateFlagsSpec(String flagsSpec);

    @NonNull
    Set<String> getNamesByFlagSets(Collection<String> flagSets);
}
