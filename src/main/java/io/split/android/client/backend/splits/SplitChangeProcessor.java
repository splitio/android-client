package io.split.android.client.backend.splits;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.splits.ProcessedSplitChange;

public class SplitChangeProcessor {
    public ProcessedSplitChange process(SplitChange splitChange) {
        if (splitChange == null || splitChange.splits == null) {
            return new ProcessedSplitChange(new ArrayList<>(), new ArrayList<>(), -1L);
        }

        List<Split> activeSplits = new ArrayList<>();
        List<Split> archivedSplits = new ArrayList<>();
        List<Split> splits = splitChange.splits;
        for (Split split : splits) {
            if (split.name == null) {
                continue;
            }
            if (split.status == Status.ACTIVE) {
                activeSplits.add(split);
            } else {
                archivedSplits.add(split);
            }
        }
        return new ProcessedSplitChange(activeSplits, archivedSplits, splitChange.till);
    }
}
