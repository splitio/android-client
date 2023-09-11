package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.utils.logger.Logger;

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
        Logger.v("Processing with names");
        // If the feature flag name is in the filter, we process it according to its status. Otherwise it is ignored
        if (mConfiguredValues.contains(featureFlag.name)) {
            mStatusProcessStrategy.process(activeFeatureFlags, archivedFeatureFlags, featureFlag);
        }
    }
}

class SetsProcessStrategy implements FeatureFlagProcessStrategy {

    private final List<String> mConfiguredValues;
    private final StatusProcessStrategy mStatusProcessStrategy;

    SetsProcessStrategy(@NonNull List<String> configuredValues, @NonNull StatusProcessStrategy statusProcessStrategy) {
        mConfiguredValues = configuredValues;
        mStatusProcessStrategy = statusProcessStrategy;
    }

    @Override
    public void process(List<Split> activeFeatureFlags, List<Split> archivedFeatureFlags, Split featureFlag) {
        Logger.v("Processing with sets");
        if (featureFlag.sets == null || featureFlag.sets.isEmpty()) {
            archivedFeatureFlags.add(featureFlag);
            return;
        }

        boolean shouldArchive = true;
        for (String set : featureFlag.sets) {
            if (mConfiguredValues.contains(set)) {
                featureFlag.sets.retainAll(mConfiguredValues); // Remove all sets that don't match the configured sets
                // Since the feature flag has at least one set that matches the configured sets,
                // we process it according to its status
                mStatusProcessStrategy.process(activeFeatureFlags, archivedFeatureFlags, featureFlag);
                shouldArchive = false;
                break;
            }
        }

        if (shouldArchive) {
            archivedFeatureFlags.add(featureFlag);
        }
    }
}
