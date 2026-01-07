package io.split.android.client.events;

import androidx.annotation.Nullable;

import io.split.android.client.events.metadata.EventMetadata;

public interface ISplitEventsManager {

    void notifyInternalEvent(SplitInternalEvent internalEvent);

    /**
     * Notifies an internal event with metadata.
     *
     * @param internalEvent the internal event
     * @param metadata      the event metadata, can be null
     */
    void notifyInternalEvent(SplitInternalEvent internalEvent, @Nullable EventMetadata metadata);
}
