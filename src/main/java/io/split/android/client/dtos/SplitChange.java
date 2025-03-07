package io.split.android.client.dtos;

import androidx.annotation.VisibleForTesting;

import java.util.List;

public class SplitChange {
    public List<Split> splits;
    public long since;
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
