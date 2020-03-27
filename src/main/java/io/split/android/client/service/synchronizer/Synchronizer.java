package io.split.android.client.service.synchronizer;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;

public interface Synchronizer {
    void doInitialLoadFromCache();

    void synchronizeSplits(long since);

    void syncronizeMySegments();

    void startPeriodicFetching();

    void stopPeriodicFetching();

    void startPeriodicRecording();

    void pushEvent(Event event);

    void pushImpression(Impression impression);

    void pause();

    void resume();

    void stop();
}
