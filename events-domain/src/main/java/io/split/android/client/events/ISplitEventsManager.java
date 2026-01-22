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

    /**
     * Checks if an external event has already been triggered.
     *
     * @param event the event to check
     * @return true if the event has already been triggered (reached its max executions), false otherwise
     */
    boolean eventAlreadyTriggered(SplitEvent event);
}
