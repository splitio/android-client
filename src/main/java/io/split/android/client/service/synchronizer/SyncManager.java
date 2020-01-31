package io.split.android.client.service.synchronizer;

import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;

public interface SyncManager {
    void start();

    void pause();

    void resume();

    void stop();

    void flush();

    void pushEvent(Event event);

    void pushImpression(Impression impression);
}
