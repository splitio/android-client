package io.split.android.client.service.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.core.util.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.impressions.DecoratedImpression;
import io.split.android.client.service.impressions.strategy.PeriodicTracker;
import io.split.android.client.service.impressions.strategy.ProcessStrategy;
import io.split.android.client.utils.logger.Logger;

public class StrategyImpressionManager implements ImpressionManager, PeriodicTracker {

    private final AtomicBoolean isTrackingEnabled = new AtomicBoolean(true);
    private final ProcessStrategy mProcessStrategy;
    private final ProcessStrategy mNoneStrategy;
    private final Set<PeriodicTracker> mPeriodicTrackers;

    public StrategyImpressionManager(Pair<ProcessStrategy, PeriodicTracker> noneComponents, Pair<ProcessStrategy, PeriodicTracker> strategy) {
        this(noneComponents.first, noneComponents.second, strategy.first, strategy.second);
    }

    StrategyImpressionManager(ProcessStrategy noneStrategy, PeriodicTracker noneTracker, ProcessStrategy strategy, PeriodicTracker strategyTracker) {
        mProcessStrategy = checkNotNull(strategy);
        mNoneStrategy = checkNotNull(noneStrategy);
        mPeriodicTrackers = new HashSet<>();
        mPeriodicTrackers.add(noneTracker);
        mPeriodicTrackers.add(strategyTracker);
    }

    @Override
    public void pushImpression(DecoratedImpression impression) {
        if (!isTrackingEnabled.get()) {
            Logger.v("Impression not tracked because tracking is disabled");
            return;
        }

        if (impression.isImpressionsDisabled()) {
            mNoneStrategy.apply(impression.getImpression());
        } else {
            mProcessStrategy.apply(impression.getImpression());
        }
    }

    @Override
    public void enableTracking(boolean enable) {
        isTrackingEnabled.set(enable);
    }

    @Override
    public void flush() {
        for (PeriodicTracker tracker : mPeriodicTrackers) {
            tracker.flush();
        }
    }

    @Override
    public void startPeriodicRecording() {
        for (PeriodicTracker tracker : mPeriodicTrackers) {
            tracker.startPeriodicRecording();
        }
    }

    @Override
    public void stopPeriodicRecording() {
        for (PeriodicTracker tracker : mPeriodicTrackers) {
            tracker.stopPeriodicRecording();
        }
    }
}
