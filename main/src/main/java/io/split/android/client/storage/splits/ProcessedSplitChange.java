package io.split.android.client.storage.splits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.Split;

public class ProcessedSplitChange {
    private final List<Split> activeSplits;
    private final List<Split> archivedSplits;
    private final long changeNumber;
    private final long updateTimestamp;

    public ProcessedSplitChange(List<Split> activeSplits, List<Split> archivedSplits, long changeNumber, long updateTimestamp) {
        // Create defensive copies to ensure thread safety
        this.activeSplits = activeSplits != null ? Collections.unmodifiableList(new ArrayList<>(activeSplits)) : Collections.emptyList();
        this.archivedSplits = archivedSplits != null ? Collections.unmodifiableList(new ArrayList<>(archivedSplits)) : Collections.emptyList();
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
