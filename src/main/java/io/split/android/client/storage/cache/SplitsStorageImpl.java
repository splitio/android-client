package io.split.android.client.storage.cache;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;

public class SplitsStorageImpl implements SplitsStorage {
    @Override
    public Split get(String name) {
        return null;
    }

    @Override
    public Map<String, Split> getMany(List<Split> splits) {
        return null;
    }

    @Override
    public void putSplit(String name, Split split) {

    }

    @Override
    public boolean isValidTrafficType(String name) {
        return false;
    }

    @Override
    public long getTill() {
        return 0;
    }

    @Override
    public long getTill(long since) {
        return 0;
    }

    @Override
    public void clear() {

    }
}
