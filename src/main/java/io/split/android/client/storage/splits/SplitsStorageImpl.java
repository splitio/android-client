package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsStorageImpl implements SplitsStorage {

    private PersistentSplitsStorage mPersistentStorage;
    private Map<String, Split> mInMemorySplits;
    private long mChangeNumber;
    private long mUpdateTimestamp;
    private String mSplitsFilterQueryString;
    private Map<String, Integer> mTrafficTypes;

    public SplitsStorageImpl(@NonNull PersistentSplitsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mInMemorySplits = new ConcurrentHashMap<String, Split>();
        mTrafficTypes = new ConcurrentHashMap<String, Integer>();
    }

    @Override
    public void loadLocal() {
        SplitsSnapshot snapshot = mPersistentStorage.getSnapshot();
        List<Split> splits = snapshot.getSplits();
        mChangeNumber = snapshot.getChangeNumber();
        mUpdateTimestamp = snapshot.getUpdateTimestamp();
        mSplitsFilterQueryString = snapshot.getSplitsFilterQueryString();
        for (Split split : splits) {
            mInMemorySplits.put(split.name, split);
        }
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
    public void update(ProcessedSplitChange splitChange) {
        if(splitChange == null) {
            return;
        }

        List<Split> activeSplits = splitChange.getActiveSplits();
        List<Split> archivedSplits = splitChange.getArchivedSplits();
        if(activeSplits != null) {
            for (Split split : activeSplits) {
                Split loadedSplit = mInMemorySplits.get(split.name);
                if (loadedSplit != null && loadedSplit.trafficTypeName != null) {
                    decreaseTrafficTypeCount(loadedSplit.trafficTypeName);
                }
                increaseTrafficTypeCount(split.trafficTypeName);
                mInMemorySplits.put(split.name, split);
            }
        }

        if(archivedSplits != null) {
            for (Split split : archivedSplits) {
                if(mInMemorySplits.remove(split.name) != null) {
                    decreaseTrafficTypeCount(split.trafficTypeName);
                }
            }
        }

        mChangeNumber = splitChange.getChangeNumber();
        mUpdateTimestamp = splitChange.getUpdateTimestamp();
        mPersistentStorage.update(splitChange);
    }

    @Override
    public void updateWithoutChecks(Split split) {
        mInMemorySplits.put(split.name, split);
        mPersistentStorage.update(split);
    }

    @Override
    public long getTill() {
        return mChangeNumber;
    }

    @Override
    public long getUpdateTimestamp() {
        return mUpdateTimestamp;
    }

    public String getSplitsFilterQueryString() {
        return mSplitsFilterQueryString;
    }

    @Override
    public void updateSplitsFilterQueryString(String queryString) {
        mPersistentStorage.updateFilterQueryString(queryString);
    }

    @Override
    public void clear() {
        mInMemorySplits.clear();
        mChangeNumber = -1;
        mPersistentStorage.clear();
    }

    @Override
    public boolean isValidTrafficType(String name) {
        if (name == null) {
            return false;
        }
        return (mTrafficTypes.get(name.toLowerCase()) != null);
    }

    private void increaseTrafficTypeCount(@NonNull String name) {
        if (name == null) {
            return;
        }

        String lowercaseName = name.toLowerCase();
        int count = countForTrafficType(lowercaseName);
        mTrafficTypes.put(lowercaseName, ++count);
    }

    private void decreaseTrafficTypeCount(@NonNull String name) {
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
}
