package io.split.android.client.service.impressions;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.impressions.strategy.PeriodicTracker;

public interface ImpressionManager extends PeriodicTracker {

    void pushImpression(Impression impression);

    void enableTracking(boolean enable);
}
