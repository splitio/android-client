package io.split.android.fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.cache.ISplitCache;
import io.split.android.client.dtos.Split;

public class SplitCacheStub implements ISplitCache {

    Map<String, Split> mSplits;

    public SplitCacheStub(List<Split> splits) {
        mSplits = new HashMap<>();
        for(Split split : splits) {
            mSplits.put(split.name, split);
        }
    }

    @Override
    public boolean addSplit(Split split) {
        mSplits.put(split.name, split);
        return true;
    }

    @Override
    public boolean setChangeNumber(long changeNumber) {
        return false;
    }

    @Override
    public long getChangeNumber() {
        return -1;
    }

    @Override
    public Split getSplit(String splitName) {
        return mSplits.get(splitName);
    }

    @Override
    public List<String> getSplitNames() {
        List<String> names = new ArrayList<>();
        for(Map.Entry<String, Split> entry : mSplits.entrySet()) {
            names.add(entry.getKey());
        }
        return names;
    }

    @Override
    public boolean trafficTypeExists(String trafficType) {
        return true;
    }
}
