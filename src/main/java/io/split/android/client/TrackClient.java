package io.split.android.client;

import io.split.android.client.dtos.Event;

public interface TrackClient {

    boolean track(Event event);

    void pause();

    void resume();

    void close();

    void flush();

    void saveToDisk();
}
