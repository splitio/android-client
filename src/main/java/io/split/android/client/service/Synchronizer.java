package io.split.android.client.service;

public interface Synchronizer {
    void syncAll();
    void synchronizeSplits(long since);
    void syncronizeMySegments();
    void startPeriodicFetching();
    void stopPeriodicFetching();
    void startPeriodicRecording();
    void stopPeriodicRecording();
}
