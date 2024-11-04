package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class UpdatesFromSSE {

    @SerializedName("sp")
    private long mSplits;

    @SerializedName("ms")
    private long mMySegments;

    @SerializedName("mls")
    private long mMyLargeSegments;

    public UpdatesFromSSE(long splits, long mySegments, long myLargeSegments) {
        mSplits = splits;
        mMySegments = mySegments;
        mMyLargeSegments = myLargeSegments;
    }

    public long getSplits() {
        return mSplits;
    }

    public long getMySegments() {
        return mMySegments;
    }

    public long getMyLargeSegments() {
        return mMyLargeSegments;
    }
}
