package io.split.android.client.service;

import io.split.android.client.dtos.Event;

public interface SyncManager {
    void start();

    void pause();

    void resume();

    void stop();

    void pushEvent(Event event);
}
