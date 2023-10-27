package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.FlagSetsFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;

interface FeatureFlagProcessStrategy {

    void process(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag);
}

class StatusProcessStrategy implements FeatureFlagProcessStrategy {

    @Override
    public void process(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.status == Status.ACTIVE) {
            activeFeatureFlags.add(featureFlag);
        } else {
            archivedFeatureFlags.add(featureFlag);
        }
    }
}

class NamesProcessStrategy implements FeatureFlagProcessStrategy {

    private final List<String> mConfiguredValues;
    private final StatusProcessStrategy mStatusProcessStrategy;

    NamesProcessStrategy(@NonNull List<String> configuredValues, @NonNull StatusProcessStrategy statusProcessStrategy) {
        mConfiguredValues = configuredValues;
        mStatusProcessStrategy = statusProcessStrategy;
    }

    @Override
    public void process(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        // If the feature flag name is in the filter, we process it according to its status. Otherwise it is ignored
        if (mConfiguredValues.contains(featureFlag.name)) {
            mStatusProcessStrategy.process(activeFeatureFlags, archivedFeatureFlags, featureFlag);
        }
    }
}

class SetsProcessStrategy implements FeatureFlagProcessStrategy {

    private final FlagSetsFilter mFlagSetsFilter;
    private final StatusProcessStrategy mStatusProcessStrategy;

    SetsProcessStrategy(@NonNull FlagSetsFilter flagSetsFilter, @NonNull StatusProcessStrategy statusProcessStrategy) {

        mStatusProcessStrategy = statusProcessStrategy;
        mFlagSetsFilter = flagSetsFilter;
    }

    @Override
    public void process(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        if (featureFlag.sets == null || featureFlag.sets.isEmpty()) {
            archivedFeatureFlags.add(featureFlag);
            return;
        }

        if (!mFlagSetsFilter.intersect(featureFlag.sets)) {
            archivedFeatureFlags.add(featureFlag);
        } else {
            mStatusProcessStrategy.process(activeFeatureFlags, archivedFeatureFlags, featureFlag);
        }
    }
}
