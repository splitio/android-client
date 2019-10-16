package io.split.android.client.cache;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;

/**
 * Created by guillermo on 11/23/17.
 */

public class SplitChangeCache implements ISplitChangeCache {

    private ISplitCache mSplitCache;

    public SplitChangeCache(ISplitCache splitCache) {
        this.mSplitCache = splitCache;
    }

    @Override
    public boolean addChange(SplitChange splitChange) {
        if (mSplitCache == null) {
            return false;
        }
        boolean result = true;
        mSplitCache.setChangeNumber(splitChange.till);
        for (Split split : splitChange.splits) {
            result = result && mSplitCache.addSplit(split);
        }
        return result;
    }

    @Override
    public SplitChange getChanges(long since) {

        long changeNumber = mSplitCache.getChangeNumber();

        SplitChange splitChange = new SplitChange();

        splitChange.splits = new ArrayList<>();
        splitChange.since = changeNumber;
        splitChange.till = changeNumber;

        if (since == -1 || since < changeNumber) {
            addAllSplits(splitChange.splits);
        }

        return splitChange;
    }

    private void addAllSplits(List<Split> splits) {
        for (String splitName : mSplitCache.getSplitNames()) {
            Split cachedSplit = mSplitCache.getSplit(splitName);
            if (cachedSplit != null) {
                splits.add(cachedSplit);
            }
        }
    }
}
