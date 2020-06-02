package io.split.android.client.storage.splits;

import java.util.List;

import io.split.android.client.dtos.Split;

public class ProcessedSplitChange {
    private final List<Split> activeSplits;
    private final List<Split> archivedSplits;
    private final long changeNumber;
    private final long updateTimestamp;

    public ProcessedSplitChange(List<Split> activeSplits, List<Split> archivedSplits, long changeNumber, long updateTimestamp) {
        this.activeSplits = activeSplits;
        this.archivedSplits = archivedSplits;
        this.changeNumber = changeNumber;
        this.updateTimestamp = updateTimestamp;
    }

    public List<Split> getActiveSplits() {
        return activeSplits;
    }

    public List<Split> getArchivedSplits() {
        return archivedSplits;
    }

    public long getChangeNumber() {
        return changeNumber;
    }

    public long getUpdateTimestamp() {
        return updateTimestamp;
    }
}
