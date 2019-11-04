package io.split.android.client.storage.cache;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;

public interface SplitsStorage {
    Split get(String name);
    Map<String, Split> getMany(List<Split> splits);
    void putSplit(String name, Split split);
    boolean isValidTrafficType(String name);
    long getTill();
    long getTill(long since);
    void clear();
}
