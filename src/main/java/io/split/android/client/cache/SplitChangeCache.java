package io.split.android.client.cache;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;

/**
 * Created by guillermo on 11/23/17.
 */

public class SplitChangeCache implements ISplitChangeCache {

    private ISplitCache _splitCache;

    public SplitChangeCache(IStorage storage) {
        this._splitCache = new SplitCache(storage);
    }

    @Override
    public boolean addChange(SplitChange splitChange) {
        if (_splitCache == null) return false;
        boolean result = true;
        _splitCache.setChangeNumber(splitChange.till);
        for (Split split :
                splitChange.splits) {
            result = result && _splitCache.addSplit(split.name, Json.toJson(split));
        }
        return result;
    }

    @Override
    public SplitChange getChanges(long since) {

        long changeNumber = _splitCache.getChangeNumber();

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
        for (String splitName :
                _splitCache.getSplitNames()) {
            Split split = Json.fromJson(_splitCache.getSplit(splitName), Split.class);
            splits.add(split);
        }
    }
}
