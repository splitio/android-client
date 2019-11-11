package io.split.android.client.storage.splits;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;

public class ProcessedSplitChange {
    private List<Split> activeSplits;
    private List<Split> archivedSplits;

    public ProcessedSplitChange() {
        activeSplits = new ArrayList<>();
        archivedSplits = new ArrayList<>();
    }

    public ProcessedSplitChange(List<Split> activeSplits, List<Split> archivedSplits) {
        this.activeSplits = activeSplits;
        this.archivedSplits = archivedSplits;
    }

    public List<Split> getActiveSplits() {
        return activeSplits;
    }

    public void setActiveSplits(List<Split> activeSplits) {
        this.activeSplits = activeSplits;
    }

    public List<Split> getArchivedSplits() {
        return archivedSplits;
    }

    public void setArchivedSplits(List<Split> archivedSplits) {
        this.archivedSplits = archivedSplits;
    }
}
