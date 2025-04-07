package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.JsonSyntaxException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.SplitFactoryImpl.StartupTimeTracker;
import io.split.android.client.utils.Json;

public class SplitsStorageImpl implements SplitsStorage {

    private final PersistentSplitsStorage mPersistentStorage;
    private final Map<String, Split> mInMemorySplits;
    private final Map<String, Set<String>> mFlagSets;
    private long mChangeNumber;
    private long mUpdateTimestamp;
    private String mSplitsFilterQueryString;
    private String mFlagsSpec;
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
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Starting"));
        long startTime = System.currentTimeMillis();
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Getting snapshot from persistent storage"));
        long snapshotStartTime = System.currentTimeMillis();
        SplitsSnapshot snapshot = mPersistentStorage.getSnapshot();
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Got snapshot in " + 
                (System.currentTimeMillis() - snapshotStartTime) + "ms"));
        
        List<Split> splits = snapshot.getSplits();
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Loaded " + 
                (splits != null ? splits.size() : 0) + " splits"));
        
        mChangeNumber = snapshot.getChangeNumber();
        mUpdateTimestamp = snapshot.getUpdateTimestamp();
        mSplitsFilterQueryString = snapshot.getSplitsFilterQueryString();
        mFlagsSpec = snapshot.getFlagsSpec();
        
        if (splits != null) {
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Processing splits"));
            long processingStartTime = System.currentTimeMillis();
            int count = 0;
            for (Split split : splits) {
                mInMemorySplits.put(split.name, split);
                addOrUpdateFlagSets(split);
                increaseTrafficTypeCount(split.trafficTypeName);
                count++;
                if (count % 1000 == 0) {
                    System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Processed " + count + " splits"));
                }
            }
            System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Finished processing " + 
                    count + " splits in " + (System.currentTimeMillis() - processingStartTime) + "ms"));
        }
        
        System.out.println(StartupTimeTracker.getElapsedTimeLog("SplitsStorageImpl.loadLocal: Completed in " + 
                (System.currentTimeMillis() - startTime) + "ms"));
    }

    @Override
    public Split get(@NonNull String name) {
        Split split = mInMemorySplits.get(name);
        if (split == null) {
            return null;
        }

        if (split.json == null) {
            return split;
        }

        try {
            Split parsedSplit = Json.fromJson(split.json, Split.class);
            parsedSplit.json = null;
            return mInMemorySplits.put(name, parsedSplit);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    @Override
    public Map<String, Split> getMany(@Nullable List<String> splitNames) {
        Map<String, Split> splits = new HashMap<>();
        if (splitNames == null || splitNames.isEmpty()) {
            for (String name : mInMemorySplits.keySet()) {
                splits.put(name, get(name));
            }
            return splits;
        }

        for (String name : splitNames) {
            Split split = get(name);
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
    public boolean update(ProcessedSplitChange splitChange) {
        if (splitChange == null) {
            return false;
        }

        boolean appliedUpdates = false;

        List<Split> activeSplits = splitChange.getActiveSplits();
        List<Split> archivedSplits = splitChange.getArchivedSplits();
        if (activeSplits != null) {
            if (!activeSplits.isEmpty()) {
                // There is at least one added or modified feature flag
                appliedUpdates = true;
            }
            for (Split split : activeSplits) {
                Split loadedSplit = mInMemorySplits.get(split.name);
                if (loadedSplit != null && loadedSplit.trafficTypeName != null) {
                    decreaseTrafficTypeCount(loadedSplit.trafficTypeName);
                }
                increaseTrafficTypeCount(split.trafficTypeName);
                mInMemorySplits.put(split.name, split);
                addOrUpdateFlagSets(split);
            }
        }

        if (archivedSplits != null) {
            for (Split split : archivedSplits) {
                if (mInMemorySplits.remove(split.name) != null) {
                    // The flag was in memory, so it will be updated
                    appliedUpdates = true;
                    decreaseTrafficTypeCount(split.trafficTypeName);
                    deleteFromFlagSetsIfNecessary(split);
                }
            }
        }

        mChangeNumber = splitChange.getChangeNumber();
        mUpdateTimestamp = splitChange.getUpdateTimestamp();
        mPersistentStorage.update(splitChange);

        return appliedUpdates;
    }

    @Override
    @WorkerThread
    public void updateWithoutChecks(Split split) {
        mInMemorySplits.put(split.name, split);
        mPersistentStorage.update(split);
        deleteFromFlagSets(split);
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
        mSplitsFilterQueryString = queryString;
    }

    @Override
    public String getFlagsSpec() {
        return mFlagsSpec;
    }

    @Override
    public void updateFlagsSpec(String flagsSpec) {
        mPersistentStorage.updateFlagsSpec(flagsSpec);
        mFlagsSpec = flagsSpec;
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
    public Set<String> getNamesByFlagSets(Collection<String> sets) {
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

    private void addOrUpdateFlagSets(Split split) {
        if (split.sets == null) {
            return;
        }

        for (String set : split.sets) {
            Set<String> splitsForSet = mFlagSets.get(set);
            if (splitsForSet == null) {
                splitsForSet = new HashSet<>();
                mFlagSets.put(set, splitsForSet);
            }
            splitsForSet.add(split.name);
        }

        deleteFromFlagSetsIfNecessary(split);
    }

    private void deleteFromFlagSetsIfNecessary(Split featureFlag) {
        if (featureFlag.sets == null) {
            return;
        }

        for (String set : mFlagSets.keySet()) {
            if (featureFlag.sets.contains(set)) {
                continue;
            }

            Set<String> flagsForSet = mFlagSets.get(set);
            if (flagsForSet != null) {
                flagsForSet.remove(featureFlag.name);
            }
        }
    }

    private void deleteFromFlagSets(Split featureFlag) {
        for (String set : mFlagSets.keySet()) {
            Set<String> flagsForSet = mFlagSets.get(set);
            if (flagsForSet != null) {
                flagsForSet.remove(featureFlag.name);
            }
        }
    }
}
