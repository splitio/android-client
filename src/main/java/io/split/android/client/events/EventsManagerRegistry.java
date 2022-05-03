package io.split.android.client.events;

import io.split.android.client.api.Key;

public interface EventsManagerRegistry {

    void registerEventsManager(Key key, ISplitEventsManager splitEventsManager);

    void unregisterEventsManager(Key key);
}
