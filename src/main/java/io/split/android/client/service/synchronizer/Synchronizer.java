package io.split.android.client.service.synchronizer;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.lifecycle.SplitLifecycleAware;

public interface Synchronizer extends SplitLifecycleAware {

    void loadAndSynchronizeSplits();

    void loadSplitsFromCache();

    void loadMySegmentsFromCache();

    void loadAttributesFromCache();

    void synchronizeSplits(long since);

    void synchronizeSplits();

    void synchronizeMySegments();

    void forceMySegmentsSync();

    void startPeriodicFetching();

    void stopPeriodicFetching();

    void startPeriodicRecording();

    void stopPeriodicRecording();

    void pushEvent(Event event);

    void pushImpression(Impression impression);

    void flush();

    void destroy();
}
