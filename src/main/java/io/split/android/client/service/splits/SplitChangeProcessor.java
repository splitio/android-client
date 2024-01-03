package io.split.android.client.service.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.storage.splits.ProcessedSplitChange;

public class SplitChangeProcessor {

    private final SplitFilter mSplitFilter;

    private final StatusProcessStrategy mStatusProcessStrategy;

    private final FlagSetsFilter mFlagSetsFilter;

    /** @noinspection unused*/ // Used in tests
    private SplitChangeProcessor() {
        mSplitFilter = null;
        mStatusProcessStrategy = new StatusProcessStrategy();
        mFlagSetsFilter = null;
    }

    public SplitChangeProcessor(@Nullable Map<SplitFilter.Type, SplitFilter> filters, FlagSetsFilter flagSetsFilter) {
        // We're only supporting one filter type
        if (filters == null || filters.isEmpty()) {
            mSplitFilter = null;
        } else {
            mSplitFilter = filters.values().iterator().next();
        }

        mStatusProcessStrategy = new StatusProcessStrategy();
        mFlagSetsFilter = flagSetsFilter;
    }

    public SplitChangeProcessor(@Nullable SplitFilter splitFilter, @Nullable FlagSetsFilter flagSetsFilter) {
        mSplitFilter = splitFilter;
        mFlagSetsFilter = flagSetsFilter;
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

        FeatureFlagProcessStrategy processStrategy = getProcessStrategy(mSplitFilter);

        for (Split featureFlag : featureFlags) {
            if (featureFlag == null || featureFlag.name == null) {
                continue;
            }

            processStrategy.process(activeFeatureFlags, archivedFeatureFlags, featureFlag);
        }

        return new ProcessedSplitChange(activeFeatureFlags, archivedFeatureFlags, changeNumber, System.currentTimeMillis() / 100);
    }

    private FeatureFlagProcessStrategy getProcessStrategy(SplitFilter splitFilter) {
        if (splitFilter == null || splitFilter.getValues().isEmpty()) {
            return mStatusProcessStrategy;
        }

        if (splitFilter.getType() == SplitFilter.Type.BY_SET && mFlagSetsFilter != null) {
            return new SetsProcessStrategy(mFlagSetsFilter, mStatusProcessStrategy);
        } else if (splitFilter.getType() == SplitFilter.Type.BY_NAME) {
            return new NamesProcessStrategy(splitFilter.getValues(), mStatusProcessStrategy);
        } else {
            return mStatusProcessStrategy;
        }
    }
}
