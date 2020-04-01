package io.split.android.client.service.synchronizer;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;

public interface Synchronizer {
    void loadSplitsFromCache();

    void loadMySegmentsFromCache();

    void synchronizeSplits(long since);

    void synchronizeSplits();

    void syncronizeMySegments();

    void startPeriodicFetching();

    void stopPeriodicFetching();

    void startPeriodicRecording();

    void stopPeriodicRecording();

    void pushEvent(Event event);

    void pushImpression(Impression impression);

    void flush();

    void pause();

    void resume();

    void destroy();
}
