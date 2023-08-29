package io.split.android.client.service.splits;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.splits.ProcessedSplitChange;

public class SplitChangeProcessor {

    private final Set<String> mConfiguredSets;

    @VisibleForTesting
    SplitChangeProcessor() {
        this(Collections.emptySet());
    }

    public SplitChangeProcessor(Set<String> configuredSets) {
        mConfiguredSets = configuredSets;
    }

    public ProcessedSplitChange process(SplitChange splitChange) {
        if (splitChange == null || splitChange.splits == null) {
            return new ProcessedSplitChange(new ArrayList<>(), new ArrayList<>(), -1L, 0);
        }

        return buildProcessedSplitChange(splitChange.splits, splitChange.till);
    }

    public ProcessedSplitChange process(Split featureFlag, long changeNumber) {
        return buildProcessedSplitChange(Collections.singletonList(featureFlag), changeNumber);
    }

    @NonNull
    private ProcessedSplitChange buildProcessedSplitChange(List<Split> featureFlags, long changeNumber) {
        List<Split> activeFeatureFlags = new ArrayList<>();
        List<Split> archivedFeatureFlags = new ArrayList<>();
        for (Split featureFlag : featureFlags) {
            if (featureFlag.name == null) {
                continue;
            }

            if (mConfiguredSets.isEmpty()) {
                processWithoutSets(activeFeatureFlags, archivedFeatureFlags, featureFlag);
            } else {
                processWithSets(activeFeatureFlags, archivedFeatureFlags, featureFlag);
            }
        }

        return new ProcessedSplitChange(activeFeatureFlags, archivedFeatureFlags, changeNumber, System.currentTimeMillis() / 100);
    }

    private void processWithoutSets(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.status == Status.ACTIVE) {
            activeFeatureFlags.add(featureFlag);
        } else {
            archivedFeatureFlags.add(featureFlag);
        }
    }

    private void processWithSets(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.sets == null || featureFlag.sets.isEmpty()) {
            archivedFeatureFlags.add(featureFlag);
            return;
        }

        boolean shouldArchive = true;
        for (String set : featureFlag.sets) {
            if (mConfiguredSets.contains(set)) {
                processWithoutSets(activeFeatureFlags, archivedFeatureFlags, featureFlag);
                shouldArchive = false;
                break;
            }
        }

        if (shouldArchive) {
            archivedFeatureFlags.add(featureFlag);
        }
    }
}
