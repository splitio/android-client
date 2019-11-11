package io.split.android.client.storage.splits;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;

public class ProcessedSplitChange {
    private final List<Split> activeSplits;
    private final List<Split> archivedSplits;

    public ProcessedSplitChange(List<Split> activeSplits, List<Split> archivedSplits) {
        this.activeSplits = activeSplits;
        this.archivedSplits = archivedSplits;
    }

    public List<Split> getActiveSplits() {
        return activeSplits;
    }

    public List<Split> getArchivedSplits() {
        return archivedSplits;
    }
}
