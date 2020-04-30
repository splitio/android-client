package io.split.android.client.storage.splits;

import androidx.core.util.Pair;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface PersistentSplitsStorage {
    boolean update(ProcessedSplitChange splitChange);
    SplitsSnapshot getSnapshot();
    void update(Split splitName);
    void clear();
    void close();
}