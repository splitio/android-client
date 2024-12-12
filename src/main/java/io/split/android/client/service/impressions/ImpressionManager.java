package io.split.android.client.service.impressions;

import io.split.android.client.impressions.DecoratedImpression;

public interface ImpressionManager {

    void enableTracking(boolean enable);

    void pushImpression(DecoratedImpression impression);
}
