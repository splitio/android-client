package io.split.android.client.service.impressions;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.impressions.strategy.PeriodicTracker;
import io.split.android.client.service.impressions.strategy.ProcessStrategy;
import io.split.android.client.utils.logger.Logger;

public class StrategyImpressionManager implements ImpressionManager {

    private final AtomicBoolean isTrackingEnabled = new AtomicBoolean(true);
    private final ProcessStrategy mProcessStrategy;
    private final PeriodicTracker mPeriodicTracker;

    public StrategyImpressionManager(@NonNull ProcessStrategy processStrategy) {
        this(processStrategy, processStrategy);
    }

    @VisibleForTesting
    StrategyImpressionManager(@NonNull ProcessStrategy processStrategy, @NonNull PeriodicTracker periodicTracker) {
        mProcessStrategy = checkNotNull(processStrategy);
        mPeriodicTracker = checkNotNull(periodicTracker);
    }

    @Override
    public void enableTracking(boolean enable) {
        isTrackingEnabled.set(enable);
    }

    @Override
    public void pushImpression(Impression impression) {
        if (!isTrackingEnabled.get()) {
            Logger.v("Impression not tracked because tracking is disabled");
            return;
        }

        mProcessStrategy.apply(impression);
    }

    @Override
    public void flush() {
        mPeriodicTracker.flush();
    }

    @Override
    public void startPeriodicRecording() {
        mPeriodicTracker.startPeriodicRecording();
    }

    @Override
    public void stopPeriodicRecording() {
        mPeriodicTracker.stopPeriodicRecording();
    }
}
