package io.split.android.client.storage.cache;

import androidx.core.util.Pair;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface PersistentSplitsStorage {
    boolean add(List<Split> splits);
    boolean remove(List<Split> splits);
    Pair<List<Split>, Long> getSnapshot();
    void close();
}