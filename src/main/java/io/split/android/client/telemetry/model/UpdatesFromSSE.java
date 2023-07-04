package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class UpdatesFromSSE {

    @SerializedName("sp")
    private long mSplits;

    @SerializedName("ms")
    private long mMySegments;

    public UpdatesFromSSE(long splits, long mySegments) {
        mSplits = splits;
        mMySegments = mySegments;
    }

    public long getSplits() {
        return mSplits;
    }

    public long getMySegments() {
        return mMySegments;
    }
}
