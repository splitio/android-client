package io.split.android.client.cache;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;

/**
 * Created by guillermo on 11/23/17.
 */

public class SplitCache implements ISplitCache, LifecycleObserver {

    private static final String SPLIT_FILE_PREFIX = "SPLITIO.split.";
    private static final String CHANGE_NUMBER_FILE = "SPLITIO.changeNumber";

    private final IStorage mFileStorage;

    private long mChangeNumber = -1;
    private Set<String> mRemovedSplits = null;
    private Map<String, Split> mSplits = null;

    public SplitCache(IStorage storage) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mFileStorage = storage;
        mSplits = Collections.synchronizedMap(new HashMap<String, Split>());
        mRemovedSplits = Collections.synchronizedSet(new HashSet<String>());
        loadChangeNumberFromDisk();
    }

    private String getSplitId(String splitName) {
        if (splitName.startsWith(SPLIT_FILE_PREFIX)) {
            return splitName;
        }
        return String.format("%s%s", SPLIT_FILE_PREFIX, splitName);
    }

    private String getChangeNumberFileName() {
        return CHANGE_NUMBER_FILE;
    }

    @Override
    public boolean addSplit(Split split) {
        mSplits.put(split.name, split);
        return true;
    }

    @Override
    public boolean removeSplit(String splitName) {
        mSplits.remove(splitName);
        mRemovedSplits.add(splitName);
        return true;
    }

    @Override
    public boolean setChangeNumber(long changeNumber) {
        mChangeNumber = changeNumber;
        try {
            mFileStorage.write(getChangeNumberFileName(),String.valueOf(changeNumber));
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
    public Split getSplit(String splitName) {
        Split split =  mSplits.get(splitName);
        if(split == null && !mRemovedSplits.contains(splitName)) {
            split = getSplitFromDisk(splitName);
            mSplits.put(splitName, split);
        }
        return split;
    }

    @Override
    public List<String> getSplitNames() {
        return new ArrayList(mSplits.keySet()) ;
    }

    private Split getSplitFromDisk(String splitName){
        Split split = null;

        if(splitName == null || splitName.trim().equals("")) return null;

        try {
            String splitJson = mFileStorage.read(getSplitId(splitName));
            if (splitJson != null && !splitJson.trim().equals("")) {
                split = Json.fromJson(splitJson, Split.class);
            }
        } catch (IOException e) {
            Logger.e(e, "Unable to load split from disk");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved segments");
        }
        return split;
    }

    private void loadChangeNumberFromDisk(){
        try {
            mChangeNumber = Long.parseLong(mFileStorage.read(getChangeNumberFileName()));
        } catch (Exception e) {
            Logger.d("Failed to get changeNumber", e);
            mChangeNumber = -1;
        }
    }

    // Lifecyle observer
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void writeSplitsToDisk() {
        // Save change number
        try {
            mFileStorage.write(getChangeNumberFileName(), String.valueOf(mChangeNumber));
        } catch (IOException e) {
            Logger.e(e, "Could not save splits change number: " + e.getLocalizedMessage());
        }

        // Save splits
        Set<String> splitNames = mSplits.keySet();
        for (String splitName : splitNames) {
            Split splits = mSplits.get(splitName);
            try {
                mFileStorage.write(getSplitId(splitName), Json.toJson(splits));
            } catch (IOException e) {
                Logger.e(e, "Could not save split " + splitName + " to disk: " + e.getLocalizedMessage());
            }
        }

        // Delete removed splits
        for(String splitName : mRemovedSplits) {
            try {
                mFileStorage.delete(getSplitId(splitName));
            } catch (Exception e) {
                Logger.e(e, "Could not remove split " + splitName + " to disk: " + e.getLocalizedMessage());
            }
        }
    }
}
