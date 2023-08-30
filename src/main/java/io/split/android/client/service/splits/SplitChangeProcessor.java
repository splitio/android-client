package io.split.android.client.service.splits;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public SplitChangeProcessor(@NonNull Set<String> configuredSets) {
        mConfiguredSets = checkNotNull(configuredSets);
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
                processAccordingToStatus(activeFeatureFlags, archivedFeatureFlags, featureFlag);
            } else {
                processAccordingToSets(activeFeatureFlags, archivedFeatureFlags, featureFlag);
            }
        }

        return new ProcessedSplitChange(activeFeatureFlags, archivedFeatureFlags, changeNumber, System.currentTimeMillis() / 100);
    }

    /**
     * Process the feature flag according to its status
     * @param activeFeatureFlags List of feature flags with status {@link Status#ACTIVE}
     * @param archivedFeatureFlags List of feature flags with status different than {@link Status#ACTIVE}
     * @param featureFlag Feature flag to process
     */
    private void processAccordingToStatus(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.status == Status.ACTIVE) {
            activeFeatureFlags.add(featureFlag);
        } else {
            archivedFeatureFlags.add(featureFlag);
        }
    }

    /**
     * Process the feature flag according to its sets.
     * @param activeFeatureFlags List of feature flags with sets that match the configured sets
     * @param archivedFeatureFlags List of feature flags with sets that don't match the configured sets
     * @param featureFlag Feature flag to process
     */
    private void processAccordingToSets(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.sets == null || featureFlag.sets.isEmpty()) {
            archivedFeatureFlags.add(featureFlag);
            return;
        }

        boolean shouldArchive = true;
        for (String set : featureFlag.sets) {
            if (mConfiguredSets.contains(set)) {
                // If the feature flag has at least one set that matches the configured sets,
                // we process it according to its status
                processAccordingToStatus(activeFeatureFlags, archivedFeatureFlags, featureFlag);
                shouldArchive = false;
                break;
            }
        }

        if (shouldArchive) {
            archivedFeatureFlags.add(featureFlag);
        }
    }
}
