package io.split.android.client.events;

public interface EventsManagerRegister {

    void registerEventsManager(String matchingKey, ISplitEventsManager splitEventsManager);
    void unregisterEventsManager(String matchingKey);
}
