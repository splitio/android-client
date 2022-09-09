package io.split.android.client.service.impressions;

import io.split.android.client.impressions.Impression;

public interface ImpressionManager {

    void pushImpression(Impression impression);

    void flush();

    void startPeriodicRecording();

    void stopPeriodicRecording();
}
