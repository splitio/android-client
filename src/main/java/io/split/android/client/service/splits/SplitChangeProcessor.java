package io.split.android.client.service.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.splits.ProcessedSplitChange;

public class SplitChangeProcessor {

    private final SplitFilter mSplitFilter;

    @VisibleForTesting
    SplitChangeProcessor() {
        this((SplitFilter) null);
    }

    public SplitChangeProcessor(@Nullable List<SplitFilter> filters) {
        // We're only supporting one filter type
        if (filters == null || filters.isEmpty()) {
            mSplitFilter = null;
        } else {
            mSplitFilter = filters.get(0);
        }
    }

    public SplitChangeProcessor(@Nullable SplitFilter splitFilter) {
        mSplitFilter = splitFilter;
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

            if (mSplitFilter == null || mSplitFilter.getValues().isEmpty()) {
                processAccordingToStatus(activeFeatureFlags, archivedFeatureFlags, featureFlag);
            } else {
                if (mSplitFilter.getType() == SplitFilter.Type.BY_NAME) {
                    processAccordingToNames(activeFeatureFlags, archivedFeatureFlags, featureFlag, mSplitFilter.getValues());
                } else if (mSplitFilter.getType() == SplitFilter.Type.BY_SET) {
                    processAccordingToSets(activeFeatureFlags, archivedFeatureFlags, featureFlag, mSplitFilter.getValues());
                }
            }
        }

        return new ProcessedSplitChange(activeFeatureFlags, archivedFeatureFlags, changeNumber, System.currentTimeMillis() / 100);
    }

    /**
     * Process the feature flag according to its status
     */
    private void processAccordingToStatus(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.status == Status.ACTIVE) {
            activeFeatureFlags.add(featureFlag);
        } else {
            archivedFeatureFlags.add(featureFlag);
        }
    }

    /**
     * Process the feature flag according to its name
     */
    private void processAccordingToNames(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag, List<String> configuredValues) {
        // If the feature flag name is in the filter, we process it according to its status. Otherwise it's ignored
        if (configuredValues.contains(featureFlag.name)) {
            processAccordingToStatus(activeFeatureFlags, archivedFeatureFlags, featureFlag);
        }
    }

    /**
     * Process the feature flag according to its sets
     */
    private void processAccordingToSets(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag, List<String> configuredValues) {
        if (featureFlag.sets == null || featureFlag.sets.isEmpty()) {
            archivedFeatureFlags.add(featureFlag);
            return;
        }

        boolean shouldArchive = true;
        for (String set : featureFlag.sets) {
            if (configuredValues.contains(set)) {
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
