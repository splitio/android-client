package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.splits.ProcessedSplitChange;

public class SplitChangeProcessor {
    public ProcessedSplitChange process(SplitChange splitChange) {
        if (splitChange == null || splitChange.splits == null) {
            return new ProcessedSplitChange(new ArrayList<>(), new ArrayList<>(), -1L, 0);
        }

        return buildProcessedSplitChange(splitChange.splits, splitChange.till);
    }

    public ProcessedSplitChange process(Split split, long changeNumber) {
        return buildProcessedSplitChange(Collections.singletonList(split), changeNumber);
    }

    @NonNull
    private static ProcessedSplitChange buildProcessedSplitChange(List<Split> featureFlags, long changeNumber) {
        List<Split> activeSplits = new ArrayList<>();
        List<Split> archivedSplits = new ArrayList<>();
        for (Split split : featureFlags) {
            if (split.name == null) {
                continue;
            }
            if (split.status == Status.ACTIVE) {
                activeSplits.add(split);
            } else {
                archivedSplits.add(split);
            }
        }

        return new ProcessedSplitChange(activeSplits, archivedSplits, changeNumber, System.currentTimeMillis() / 100);
    }
}
