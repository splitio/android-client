package io.split.android.client.storage.splits;

import java.util.List;

import io.split.android.client.dtos.SimpleSplit;

public class SplitsSnapshot {

    private final long mChangeNumber;
    private final List<SimpleSplit> mSplits;
    private final long mUpdateTimestamp;
    private final String mSplitsFilterQueryString;
    private final String mFlagsSpec;

    public SplitsSnapshot(List<SimpleSplit> splits, long changeNumber, long updateTimestamp, String splitsFilterQueryString, String flagsSpec) {
        mChangeNumber = changeNumber;
        mSplits = splits;
        mUpdateTimestamp = updateTimestamp;
        mSplitsFilterQueryString = splitsFilterQueryString;
        mFlagsSpec = flagsSpec;
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

    public List<SimpleSplit> getSplits() {
        return mSplits;
    }

    public String getFlagsSpec() {
        return mFlagsSpec;
    }
}
