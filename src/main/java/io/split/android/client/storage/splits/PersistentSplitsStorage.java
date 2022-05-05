package io.split.android.client.storage.splits;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface PersistentSplitsStorage {
    boolean update(ProcessedSplitChange splitChange);
    SplitsSnapshot getSnapshot();
    List<Split> getAll();
    void update(Split splitName);
    String getFilterQueryString();
    void updateFilterQueryString(String queryString);
    void delete(List<String> splitNames);
    void clear();
    void close();
}
