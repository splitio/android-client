package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;

public class SplitsSnapshot {
    private long changeNumber = -1;
    private List<Split> splits;

    public long getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(long changeNumber) {
        this.changeNumber = changeNumber;
    }

    public @NonNull List<Split> getSplits() {
        return (splits != null ? splits : new ArrayList<>());
    }

    public void setSplits(@NonNull List<Split> splits) {
        this.splits = splits;
    }
}
