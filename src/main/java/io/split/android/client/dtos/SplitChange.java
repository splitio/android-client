package io.split.android.client.dtos;

import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SplitChange {
    @SerializedName(value = "d", alternate = "splits")
    public List<Split> splits;
    @SerializedName(value = "s", alternate = "since")
    public long since;
    @SerializedName(value = "t", alternate = "till")
    public long till;

    @VisibleForTesting
    public static SplitChange create(long since, long till, List<Split> splits) {
        SplitChange splitChange = new SplitChange();
        splitChange.since = since;
        splitChange.till = till;
        splitChange.splits = splits;
        return splitChange;
    }
}
