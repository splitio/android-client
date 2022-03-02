package io.split.android.client.events;

public interface EventsManagerRegistry {

    void registerEventsManager(String matchingKey, ISplitEventsManager splitEventsManager);

    void unregisterEventsManager(String matchingKey);
}
