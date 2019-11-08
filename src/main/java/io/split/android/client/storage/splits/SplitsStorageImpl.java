package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.utils.Logger;

public class SplitsStorageImpl implements SplitsStorage {

    private PersistentSplitsStorage mStorage;
    private Map<String, Split> mInMemorySplits;
    private long mChangeNumber;
    private Map<String, Integer> mTrafficTypes;

    public SplitsStorageImpl(PersistentSplitsStorage storage) {
        mStorage = storage;
        mInMemorySplits = new ConcurrentHashMap<String, Split>();
        mTrafficTypes = new ConcurrentHashMap<String, Integer>();
        loadFromDb();
    }

    @Override
    public Split get(@NonNull String name) {
        return mInMemorySplits.get(name);
    }

    @Override
    public Map<String, Split> getMany(@NonNull List<String> splitNames) {
        Map<String, Split> splits = new HashMap<>();
        if(splitNames == null || splitNames.isEmpty()) {
            splits.putAll(mInMemorySplits);
            return splits;
        }

        for(String name : splitNames) {
            Split split = mInMemorySplits.get(name);
            if(split != null) {
                splits.put(name, split);
            }
        }
        return splits;
    }

    @Override
    public Map<String, Split> getAll() {
        return getMany(null);
    }

    @Override
    public void update(@NonNull List<Split> splits, long changeNumber) {
        if(splits == null) {
            return;
        }
        List<Split> validatedSplits = new ArrayList<>();
        for (Split split : splits) {
            if(split.name == null) {
                continue;
            }
            if(split.status == Status.ACTIVE) {
                Split loadedSplit = mInMemorySplits.get(split.name);
                if(loadedSplit != null && loadedSplit.trafficTypeName != null) {
                    removeTrafficType(loadedSplit.trafficTypeName);
                }
                addTrafficType(split.trafficTypeName);
                mInMemorySplits.put(split.name, split);
            } else {
                mInMemorySplits.remove(split.name);
                removeTrafficType(split.trafficTypeName);
            }
            validatedSplits.add(split);
        }
        mChangeNumber = changeNumber;
        mStorage.update(validatedSplits, changeNumber);
    }

    @Override
    public long getTill() {
        return mChangeNumber;
    }

    @Override
    public void clear() {
        mInMemorySplits.clear();
    }

    @Override
    public boolean isValidTrafficType(String name) {
        if (name == null) {
            return false;
        }
        return (mTrafficTypes.get(name.toLowerCase()) != null);
    }

    private void addTrafficType(@NonNull String name) {
        if (name == null) {
            return;
        }

        String lowercaseName = name.toLowerCase();
        int count = countForTrafficType(lowercaseName);
        mTrafficTypes.put(lowercaseName, ++count);
    }

    private void removeTrafficType(@NonNull String name) {
        if (name == null) {
            return;
        }
        String lowercaseName = name.toLowerCase();

        int count = countForTrafficType(lowercaseName);
        if (count > 1) {
            mTrafficTypes.put(lowercaseName, --count);
        } else {
            mTrafficTypes.remove(lowercaseName);
        }
    }

    private int countForTrafficType(@NonNull String name) {
        int count = 0;
        Integer countValue = mTrafficTypes.get(name);
        if (countValue != null) {
            count = countValue;
        }
        return count;
    }


    private void loadFromDb() {
        SplitsSnapshot snapshot = mStorage.getSnapshot();
        List<Split> splits = snapshot.getSplits();
        mChangeNumber = snapshot.getChangeNumber();
        for (Split split : splits) {
            mInMemorySplits.put(split.name, split);
        }
    }
}
