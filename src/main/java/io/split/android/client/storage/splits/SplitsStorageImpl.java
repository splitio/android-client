package io.split.android.client.storage.splits;

import static io.split.android.client.storage.splits.MetadataHelper.addOrUpdateFlagSets;
import static io.split.android.client.storage.splits.MetadataHelper.decreaseTrafficTypeCount;
import static io.split.android.client.storage.splits.MetadataHelper.deleteFromFlagSets;
import static io.split.android.client.storage.splits.MetadataHelper.deleteFromFlagSetsIfNecessary;
import static io.split.android.client.storage.splits.MetadataHelper.increaseTrafficTypeCount;
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
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Json;

public class SplitsStorageImpl implements SplitsStorage {

    private final PersistentSplitsStorage mPersistentStorage;
    private final Map<String, Split> mInMemorySplits;
    private final Map<String, Set<String>> mFlagSets;
    private long mChangeNumber;
    private String mSplitsFilterQueryString;
    private String mFlagsSpec;
    private final Map<String, Integer> mTrafficTypes;
    private final AtomicBoolean mInitialized;

    public SplitsStorageImpl(@NonNull PersistentSplitsStorage persistentStorage) {
        mInitialized = new AtomicBoolean(false);
        mPersistentStorage = checkNotNull(persistentStorage);
        mInMemorySplits = new ConcurrentHashMap<>();
        mTrafficTypes = new ConcurrentHashMap<>();
        mFlagSets = new ConcurrentHashMap<>();
    }

    @Override
    @WorkerThread
    public synchronized void loadLocal() {
        if (mInitialized.get()) {
            return;
        }

        try {
            SplitsSnapshot snapshot = mPersistentStorage.getSnapshot();
            List<Split> splits = snapshot.getSplits();

            mChangeNumber = snapshot.getChangeNumber();
            mSplitsFilterQueryString = snapshot.getSplitsFilterQueryString();
            mFlagsSpec = snapshot.getFlagsSpec();

            // Populate traffic types and flag sets
            mTrafficTypes.putAll(snapshot.getTrafficTypesMap());
            for (Map.Entry<String, Set<String>> entry : snapshot.getFlagSetsMap().entrySet()) {
                mFlagSets.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }

            for (Split split : splits) {
                mInMemorySplits.put(split.name, split);
            }
        } finally {
            mInitialized.compareAndSet(false, true);
        }
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
            mInMemorySplits.put(name, parsedSplit);
            return mInMemorySplits.get(name);
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
                Split loadedSplit = get(split.name);
                if (loadedSplit != null && loadedSplit.trafficTypeName != null) {
                    decreaseTrafficTypeCount(loadedSplit.trafficTypeName, mTrafficTypes);
                }
                increaseTrafficTypeCount(split.trafficTypeName, mTrafficTypes);
                mInMemorySplits.put(split.name, split);
                addOrUpdateFlagSets(split, mFlagSets);
            }
        }

        if (archivedSplits != null) {
            for (Split split : archivedSplits) {
                if (mInMemorySplits.remove(split.name) != null) {
                    // The flag was in memory, so it will be updated
                    appliedUpdates = true;
                    decreaseTrafficTypeCount(split.trafficTypeName, mTrafficTypes);
                    deleteFromFlagSetsIfNecessary(split, mFlagSets);
                }
            }
        }

        mChangeNumber = splitChange.getChangeNumber();

        mPersistentStorage.update(splitChange, mTrafficTypes, mFlagSets);

        return appliedUpdates;
    }

    @Override
    @WorkerThread
    public void updateWithoutChecks(Split split) {
        mInMemorySplits.put(split.name, split);
        mPersistentStorage.update(split);
        deleteFromFlagSets(split, mFlagSets);
    }

    @Override
    public long getTill() {
        return mChangeNumber;
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
}
