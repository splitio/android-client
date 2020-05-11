package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;

public class SplitsSnapshot {

    final private long changeNumber;
    final private List<Split> splits;
    final private long updateTimestamp;

    public SplitsSnapshot(List<Split> splits, long changeNumber, long updateTimestamp) {
        this.changeNumber = changeNumber;
        this.splits = splits;
        this.updateTimestamp = updateTimestamp;
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    public long getUpdateTimestamp() {
        return updateTimestamp;
    }

    public @NonNull List<Split> getSplits() {
        return (splits != null ? splits : new ArrayList<>());
    }
}
