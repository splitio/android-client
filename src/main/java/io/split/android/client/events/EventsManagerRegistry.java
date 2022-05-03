package io.split.android.client.events;

public interface EventsManagerRegistry {

    void registerEventsManager(String matchingKey, String bucketingKey, ISplitEventsManager splitEventsManager);

    void unregisterEventsManager(String matchingKey, String bucketingKey);
}
