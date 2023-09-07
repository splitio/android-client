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

    private final StatusProcessStrategy mStatusProcessStrategy;

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

        mStatusProcessStrategy = new StatusProcessStrategy();
    }

    public SplitChangeProcessor(@Nullable SplitFilter splitFilter) {
        mSplitFilter = splitFilter;
        mStatusProcessStrategy = new StatusProcessStrategy();
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

        SplitFilter.Type filterType = (mSplitFilter != null) ? mSplitFilter.getType() : null;
        List<String> filterValues = (mSplitFilter != null) ? mSplitFilter.getValues() : null;

        FeatureFlagProcessStrategy processStrategy = getProcessStrategy(filterType, filterValues);

        for (Split featureFlag : featureFlags) {
            if (featureFlag.name == null) {
                continue;
            }
            processStrategy.process(activeFeatureFlags, archivedFeatureFlags, featureFlag, filterValues);
        }

        return new ProcessedSplitChange(activeFeatureFlags, archivedFeatureFlags, changeNumber, System.currentTimeMillis() / 100);
    }

    private FeatureFlagProcessStrategy getProcessStrategy(SplitFilter.Type filterType, List<String> filterValues) {
        if (filterType == SplitFilter.Type.BY_SET) {
            return new SetsProcessStrategy(filterValues, mStatusProcessStrategy);
        } else if (filterType == SplitFilter.Type.BY_NAME) {
            return new NamesProcessStrategy(filterValues, mStatusProcessStrategy);
        } else {
            return mStatusProcessStrategy;
        }
    }
}
