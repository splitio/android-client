package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface ITrafficTypesCache {
    public void setFromSplits(List<Split> splits);
    public void updateFromSplits(List<Split> splits);
    public boolean contains(String name);
}
