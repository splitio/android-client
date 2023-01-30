package io.split.android.client.service.impressions.strategy;

public interface PeriodicTracker {

    void flush();

    void startPeriodicRecording();

    void stopPeriodicRecording();

    void enableTracking(boolean enable);
}
