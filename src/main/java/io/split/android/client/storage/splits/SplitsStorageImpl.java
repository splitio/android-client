package io.split.android.client.storage.splits;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;

public class SplitsStorageImpl implements SplitsStorage {

    private final PersistentSplitsStorage mPersistentStorage;
    private final Map<String, Split> mInMemorySplits;
    private final Map<String, Set<String>> mFlagSets;
    private long mChangeNumber;
    private long mUpdateTimestamp;
    private String mSplitsFilterQueryString;
    private final Map<String, Integer> mTrafficTypes;

    public SplitsStorageImpl(@NonNull PersistentSplitsStorage persistentStorage) {
        mPersistentStorage = checkNotNull(persistentStorage);
        mInMemorySplits = new ConcurrentHashMap<>();
        mTrafficTypes = new ConcurrentHashMap<>();
        mFlagSets = new ConcurrentHashMap<>();
    }

    @Override
    @WorkerThread
    public void loadLocal() {
        SplitsSnapshot snapshot = mPersistentStorage.getSnapshot();
        List<Split> splits = snapshot.getSplits();
        mChangeNumber = snapshot.getChangeNumber();
        mUpdateTimestamp = snapshot.getUpdateTimestamp();
        mSplitsFilterQueryString = snapshot.getSplitsFilterQueryString();
        for (Split split : splits) {
            mInMemorySplits.put(split.name, split);
            cacheFlags(split);
            increaseTrafficTypeCount(split.trafficTypeName);
        }
    }

    @Override
    public Split get(@NonNull String name) {
        return mInMemorySplits.get(name);
    }

    @Override
    public Map<String, Split> getMany(@Nullable List<String> splitNames) {
        Map<String, Split> splits = new HashMap<>();
        if (splitNames == null || splitNames.isEmpty()) {
            splits.putAll(mInMemorySplits);
            return splits;
        }

        for (String name : splitNames) {
            Split split = mInMemorySplits.get(name);
            if (split != null) {
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
    @WorkerThread
    public void update(ProcessedSplitChange splitChange) {
        if (splitChange == null) {
            return;
        }

        List<Split> activeSplits = splitChange.getActiveSplits();
        List<Split> archivedSplits = splitChange.getArchivedSplits();
        if (activeSplits != null) {
            for (Split split : activeSplits) {
                Split loadedSplit = mInMemorySplits.get(split.name);
                if (loadedSplit != null && loadedSplit.trafficTypeName != null) {
                    decreaseTrafficTypeCount(loadedSplit.trafficTypeName);
                }
                increaseTrafficTypeCount(split.trafficTypeName);
                mInMemorySplits.put(split.name, split);
            }
        }

        if (archivedSplits != null) {
            for (Split split : archivedSplits) {
                if (mInMemorySplits.remove(split.name) != null) {
                    decreaseTrafficTypeCount(split.trafficTypeName);
                }
            }
        }

        mChangeNumber = splitChange.getChangeNumber();
        mUpdateTimestamp = splitChange.getUpdateTimestamp();
        mPersistentStorage.update(splitChange);
    }

    @Override
    @WorkerThread
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
    @WorkerThread
    public void updateSplitsFilterQueryString(String queryString) {
        mPersistentStorage.updateFilterQueryString(queryString);
    }

    @Override
    @WorkerThread
    public void clear() {
        mInMemorySplits.clear();
        mChangeNumber = -1;
        mPersistentStorage.clear();
        mFlagSets.clear();
        mTrafficTypes.clear();
    }

    @NonNull
    @Override
    public Set<String> getNamesByFlagSets(List<String> sets) {
        Set<String> namesToReturn = new HashSet<>();
        if (sets == null || sets.isEmpty()) {
            return namesToReturn;
        }

        for (String set : sets) {
            Set<String> splits = mFlagSets.get(set);
            if (splits != null) {
                namesToReturn.addAll(splits);
            }
        }
        return namesToReturn;
    }

    @Override
    public boolean isValidTrafficType(@Nullable String name) {
        if (name == null) {
            return false;
        }
        return (mTrafficTypes.get(name.toLowerCase()) != null);
    }

    private void increaseTrafficTypeCount(@Nullable String name) {
        if (name == null) {
            return;
        }

        String lowercaseName = name.toLowerCase();
        int count = countForTrafficType(lowercaseName);
        mTrafficTypes.put(lowercaseName, ++count);
    }

    private void decreaseTrafficTypeCount(@Nullable String name) {
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

    private void cacheFlags(Split split) {
        Set<String> sets = split.sets;
        if (sets != null) {
            for (String set : sets) {
                Set<String> splitsForSet = mFlagSets.get(set);
                if (splitsForSet == null) {
                    splitsForSet = new HashSet<>();
                    mFlagSets.put(set, splitsForSet);
                }
                splitsForSet.add(split.name);
            }
        }
    }
}
