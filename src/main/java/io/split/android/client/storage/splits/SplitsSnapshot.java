package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;

public class SplitsSnapshot {

    private final long mChangeNumber;
    private final List<Split> mSplits;
    private final long mUpdateTimestamp;
    private final String mSplitsFilterQueryString;
    private final String mFlagsSpec;

    public SplitsSnapshot(List<Split> splits, long changeNumber, long updateTimestamp, String splitsFilterQueryString, String flagsSpec) {
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

    public @NonNull List<Split> getSplits() {
        return (mSplits != null ? mSplits : new ArrayList<>());
    }

    public String getFlagsSpec() {
        return mFlagsSpec;
    }
}
