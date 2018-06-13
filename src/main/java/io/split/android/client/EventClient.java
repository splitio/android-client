package io.split.android.client;

import io.split.android.client.dtos.Event;

public interface EventClient {

    boolean track(Event event);

    void close();
}
