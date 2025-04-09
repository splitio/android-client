package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.Split;

public class SplitsSnapshot {

    private final long mChangeNumber;
    private final List<Split> mSplits;
    private final long mUpdateTimestamp;
    private final String mSplitsFilterQueryString;
    private final String mFlagsSpec;
    private final Map<String, Integer> mTrafficTypesMap;
    private final Map<String, Set<String>> mFlagSetsMap;

    public SplitsSnapshot(List<Split> splits, long changeNumber, long updateTimestamp, 
                         String splitsFilterQueryString, String flagsSpec,
                         Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
        mChangeNumber = changeNumber;
        mSplits = splits;
        mUpdateTimestamp = updateTimestamp;
        mSplitsFilterQueryString = splitsFilterQueryString;
        mFlagsSpec = flagsSpec;
        mTrafficTypesMap = trafficTypesMap != null ? trafficTypesMap : new HashMap<>();
        mFlagSetsMap = flagSetsMap != null ? flagSetsMap : new HashMap<>();
    }

    public SplitsSnapshot(List<Split> splits, long changeNumber, long updateTimestamp, 
                         String splitsFilterQueryString, String flagsSpec) {
        this(splits, changeNumber, updateTimestamp, splitsFilterQueryString, flagsSpec, 
             new HashMap<>(), new HashMap<>());
    }

    public long getChangeNumber() {
        return mChangeNumber;
    }

    public long getUpdateTimestamp() {
        return mUpdateTimestamp;
    }

    public String getSplitsFilterQueryString() {
        return mSplitsFilterQueryString;
    }

    public @NonNull List<Split> getSplits() {
        return (mSplits != null ? mSplits : new ArrayList<>());
    }

    public String getFlagsSpec() {
        return mFlagsSpec;
    }
    
    public @NonNull Map<String, Integer> getTrafficTypesMap() {
        return mTrafficTypesMap;
    }
    
    public @NonNull Map<String, Set<String>> getFlagSetsMap() {
        return mFlagSetsMap;
    }
}
