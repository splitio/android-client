package io.split.android.client.storage.cache;

import androidx.core.util.Pair;

import java.util.List;

import io.split.android.client.dtos.Split;

public class PersistentSplitsStorageImpl implements PersistentSplitsStorage {
    @Override
    public boolean add(List<Split> splits) {
        return false;
    }

    @Override
    public boolean remove(List<Split> splits) {
        return false;
    }

    @Override
    public Pair<List<Split>, Long> getSnapshot() {
        return null;
    }

    @Override
    public void close() {

    }
}
