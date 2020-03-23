package io.split.android.client.service;

public interface Synchronizer {
    void syncAll();
    void synchronizeSplits();
    void syncronizeMySegments();
    void startPeriodicFetching();
    void stopPeriodicFetching();
    void startPeriodicRecording();
    void stopPeriodicRecording();
}
