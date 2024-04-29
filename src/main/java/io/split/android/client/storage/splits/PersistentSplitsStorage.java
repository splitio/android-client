package io.split.android.client.storage.splits;

import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface PersistentSplitsStorage {
    boolean update(ProcessedSplitChange splitChange);
    SplitsSnapshot getSnapshot();
    List<Split> getAll();

    void update(Split splitName);
    @Nullable String getFilterQueryString();
    void updateFilterQueryString(String queryString);
    @Nullable String getFlagsSpec();
    void updateFlagsSpec(String flagsSpec);
    void delete(List<String> splitNames);
    void clear();
    void close();
}
