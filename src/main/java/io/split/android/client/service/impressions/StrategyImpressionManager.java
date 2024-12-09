package io.split.android.client.service.impressions;

import androidx.core.util.Pair;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.impressions.strategy.PeriodicTracker;
import io.split.android.client.service.impressions.strategy.ProcessStrategy;
import io.split.android.client.utils.logger.Logger;

public class StrategyImpressionManager implements ImpressionManager, PeriodicTracker {

    private final AtomicBoolean isTrackingEnabled = new AtomicBoolean(true);
    private final ProcessStrategy mProcessStrategy;
    private final ProcessStrategy mNoneStrategy;
    private final PeriodicTracker[] mPeriodicTracker;

    public StrategyImpressionManager(Pair<ProcessStrategy, PeriodicTracker> noneComponents, Pair<ProcessStrategy, PeriodicTracker> strategy) {
        mProcessStrategy = strategy.first;
        mNoneStrategy = noneComponents.first;
        mPeriodicTracker = new PeriodicTracker[]{noneComponents.second, strategy.second};
    }

    @Override
    public void pushImpression(Impression impression) {
        if (!isTrackingEnabled.get()) {
            Logger.v("Impression not tracked because tracking is disabled");
            return;
        }

        if (track(impression)) {
            mProcessStrategy.apply(impression);
        } else {
            mNoneStrategy.apply(impression);
        }
    }

    @Override
    public void enableTracking(boolean enable) {
        isTrackingEnabled.set(enable);
    }

    @Override
    public void flush() {
        for (PeriodicTracker tracker : mPeriodicTracker) {
            tracker.flush();
        }
    }

    @Override
    public void startPeriodicRecording() {
        for (PeriodicTracker tracker : mPeriodicTracker) {
            tracker.startPeriodicRecording();
        }
    }

    @Override
    public void stopPeriodicRecording() {
        for (PeriodicTracker tracker : mPeriodicTracker) {
            tracker.stopPeriodicRecording();
        }
    }

    private static boolean track(Impression impression) {
        return true; // TODO: Placeholder method
    }
}
