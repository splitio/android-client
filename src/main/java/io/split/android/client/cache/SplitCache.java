package io.split.android.client.cache;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.support.annotation.VisibleForTesting;

import com.google.common.base.Strings;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 11/23/17.
 */

public class SplitCache implements ISplitCache, LifecycleObserver {

    private static final String SPLIT_FILE_PREFIX = "SPLITIO.split.";
    private static final String CHANGE_NUMBER_FILE = "SPLITIO.changeNumber";

    private final IStorage mFileStorageManager;

    private long mChangeNumber = -1;
    private Set<String> mRemovedSplits = null;
    private Map<String, Split> mInMemorySplits = null;
    private Map<String, Integer> mTrafficTypes = null;

    public SplitCache(IStorage storage) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mFileStorageManager = storage;
        mInMemorySplits = new ConcurrentHashMap<String, Split>();
        mRemovedSplits = Collections.synchronizedSet(new HashSet<>());
        mTrafficTypes = new ConcurrentHashMap<String, Integer>();
        loadChangeNumberFromDisk();
        loadSplitsFromDisk();
    }

    private String getSplitId(String splitName) {

        if (splitName.startsWith(SPLIT_FILE_PREFIX)) {
            return splitName;
        }
        return String.format("%s%s", SPLIT_FILE_PREFIX, splitName);
    }

    private String getSplitName(String fileId) {
        return fileId.replace(SPLIT_FILE_PREFIX, "");
    }

    private String getChangeNumberFileName() {
        return CHANGE_NUMBER_FILE;
    }

    @Override
    public boolean addSplit(Split split) {
        if(split == null) {
            return false;
        }

        if(split.status != null && split.status == Status.ACTIVE) {
            if(mInMemorySplits.get(split.name) == null) {
                addTrafficType(split.trafficTypeName);
            }
            mInMemorySplits.put(split.name, split);
            mRemovedSplits.remove(split.name);

        } else {
            mInMemorySplits.remove(split.name);
            mRemovedSplits.add(split.name);
            removeTrafficType(split.trafficTypeName);
        }
        return true;
    }

    @Override
    public boolean setChangeNumber(long changeNumber) {
        mChangeNumber = changeNumber;
        try {
            mFileStorageManager.write(getChangeNumberFileName(),String.valueOf(changeNumber));
            return true;
        } catch (IOException e) {
            Logger.e(e, "Failed to set changeNumber");
            return false;
        }
    }

    @Override
    public long getChangeNumber() {
        return mChangeNumber;
    }

    @Override
    synchronized public Split getSplit(String splitName) {
        return  mInMemorySplits.get(splitName);
    }

    @Override
    synchronized public List<String> getSplitNames() {
        return new ArrayList<String>(mInMemorySplits.keySet()) ;
    }

    @Override
    public boolean trafficTypeExists(String trafficType) {
        if(trafficType == null) {
            return false;
        }
        return (mTrafficTypes.get(trafficType.toLowerCase()) != null);
    }

    private void addTrafficType(@NotNull String name) {
        if(name == null) {
            return;
        }

        int count = countForTrafficType(name);
        mTrafficTypes.put(name.toLowerCase(), Integer.valueOf(count++));
    }

    private void removeTrafficType(@NotNull String name) {
        if(name == null) {
            return;
        }

        int count = countForTrafficType(name);
        if(count > 0) {
            mTrafficTypes.put(name, Integer.valueOf(count--));
        } else {
            mTrafficTypes.remove(name);
        }
    }

    private int countForTrafficType(@NotNull String name) {
        int count = 0;
        Integer countValue = mTrafficTypes.get(name);
        if(countValue != null) {
            count = countValue.intValue();
        }
        return count;
    }

    private Split getSplitFromDisk(String splitName){
        Split split = null;

        if(Strings.isNullOrEmpty(splitName)) {
            return null;
        }

        String splitId = getSplitId(splitName);

        try {
            String splitJson = mFileStorageManager.read(splitId);
            if (splitJson != null && !splitJson.trim().equals("")) {
                split = Json.fromJson(splitJson, Split.class);
            }
        } catch (IOException e) {
            Logger.e(e, "Unable to load split from disk");
        } catch (JsonSyntaxException syntaxException) {
            mFileStorageManager.delete(splitId);
            Logger.e(syntaxException, "Unable to parse saved segments");
        }
        return split;
    }

    private void loadChangeNumberFromDisk(){
        try {
            mChangeNumber = Long.parseLong(mFileStorageManager.read(getChangeNumberFileName()));
        } catch (Exception e) {
            Logger.d("Failed to get changeNumber", e);
            mChangeNumber = -1;
        }
    }

    // Lifecyle observer
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    synchronized private void writeSplitsToDisk() {
        // Save change number
        try {
            mFileStorageManager.write(getChangeNumberFileName(), String.valueOf(mChangeNumber));
        } catch (IOException e) {
            Logger.e(e, "Could not save splits change number: " + e.getLocalizedMessage());
        }

        // Save splits
        Set<String> splitNames = mInMemorySplits.keySet();
        for (String splitName : splitNames) {
            Split splits = mInMemorySplits.get(splitName);
            try {
                mFileStorageManager.write(getSplitId(splitName), Json.toJson(splits));
            } catch (IOException e) {
                Logger.e(e, "Could not save split " + splitName + " to disk: " + e.getLocalizedMessage());
            }
        }

        // Delete removed splits
        for(String splitName : mRemovedSplits) {
            try {
                mFileStorageManager.delete(getSplitId(splitName));
            } catch (Exception e) {
                Logger.e(e, "Could not remove split " + splitName + " to disk: " + e.getLocalizedMessage());
            }
        }
    }

    void loadSplitsFromDisk() {
        List<String> fileIds = mFileStorageManager.getAllIds(SPLIT_FILE_PREFIX);
        for(String fileId : fileIds) {
            Split split = getSplitFromDisk(fileId);
            if(split != null && split.name != null) {
                mInMemorySplits.put(split.name, split);
                addTrafficType(split.trafficTypeName);
            }
        }
    }

    @VisibleForTesting
    public void fireWriteToDisk() {
        writeSplitsToDisk();
    }
}
