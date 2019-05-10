package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface ITrafficTypesCache {
    void updateFromSplits(List<Split> splits);
    void updateFromSplit(Split split);
    boolean contains(String name);
}
