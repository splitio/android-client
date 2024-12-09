package io.split.android.client.service.impressions;

import io.split.android.client.impressions.Impression;

public interface ImpressionManager {

    void enableTracking(boolean enable);

    void pushImpression(Impression impression);
}
