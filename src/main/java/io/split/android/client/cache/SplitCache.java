package io.split.android.client.cache;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

public class SplitCache implements ISplitCache {

    private static final String SPLIT_FILE_PREFIX = "SPLITIO.split.";
    private static final String CHANGE_NUMBER_FILE = "SPLITIO.changeNumber";

    private final IStorage mFileStorageManager;

    private long mChangeNumber = -1;
    private Set<String> mRemovedSplits;
    private Map<String, Split> mInMemorySplits;
    private Map<String, Integer> mTrafficTypes;

    public SplitCache(IStorage storage) {
        mFileStorageManager = storage;
        mInMemorySplits = new ConcurrentHashMap<String, Split>();
        mRemovedSplits = Collections.synchronizedSet(new HashSet<>());
        mTrafficTypes = new ConcurrentHashMap<String, Integer>();
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
        if(split == null || split.name == null) {
            return false;
        }

        if(split.status != null && split.status == Status.ACTIVE) {
            Split loadedSplit = mInMemorySplits.get(split.name);
            if(loadedSplit != null && loadedSplit.trafficTypeName != null) {
                removeTrafficType(loadedSplit.trafficTypeName);
            }
            addTrafficType(split.trafficTypeName);
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
        return true;
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

        String lowercaseName = name.toLowerCase();
        int count = countForTrafficType(lowercaseName);
        mTrafficTypes.put(lowercaseName, Integer.valueOf(++count));
    }

    private void removeTrafficType(@NotNull String name) {
        if(name == null) {
            return;
        }
        String lowercaseName = name.toLowerCase();

        int count = countForTrafficType(lowercaseName);
        if(count > 1) {
            mTrafficTypes.put(lowercaseName, Integer.valueOf(--count));
        } else {
            mTrafficTypes.remove(lowercaseName);
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

    @Override
    synchronized public void saveToDisk() {

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
        long maxChangeNumber = -1;
        List<String> fileIds = mFileStorageManager.getAllIds(SPLIT_FILE_PREFIX);
        for(String fileId : fileIds) {
            Split split = getSplitFromDisk(fileId);
            if(split != null) {
                if (split.name != null) {
                    mInMemorySplits.put(split.name, split);
                    addTrafficType(split.trafficTypeName);
                }

                if (split.changeNumber > maxChangeNumber) {
                    maxChangeNumber = split.changeNumber;
                }
            }
        }
        mChangeNumber = maxChangeNumber;
    }

}
